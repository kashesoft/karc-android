/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.util.Log
import com.kashesoft.karc.core.interactor.Interactor
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.core.router.Router
import com.kashesoft.karc.utils.Logging
import dagger.android.support.DaggerApplication
import java.lang.ref.WeakReference
import javax.inject.Provider
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

abstract class Application<out R : Router> : DaggerApplication(), Logging,
        LifecycleObserver, Application.ActivityLifecycleCallbacks, Routable {

    protected open val logging = false

    private fun log(message: String) {
        Log.v(name, ":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    private var resumedActivityRef: WeakReference<Activity<*, *>>? = null
    private var startedActivities: MutableSet<Activity<*, *>> = mutableSetOf()
    private var stoppedActivityRef: WeakReference<Activity<*, *>>? = null
    private var inForeground = false
    private var isActive = false

    val resumedActivity: Activity<*, *>?
        get() = resumedActivityRef?.get()

    @CallSuper
    override fun onCreate() {
        if (logging) log("onCreate")
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)
        router.attachRoutable(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @CallSuper
    protected fun onStart() {
        if (logging) log("onStart")
        inForeground = true
        controllers.forEach { it.doEnterForeground() }
        presenters.forEach { it.doEnterForeground() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @CallSuper
    protected fun onResume() {
        if (logging) log("onResume")
        isActive = true
        controllers.forEach { it.doBecomeActive() }
        presenters.forEach { it.doBecomeActive() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @CallSuper
    protected fun onPause() {
        if (logging) log("onPause")
        isActive = false
        controllers.forEach { it.doBecomeInactive() }
        presenters.forEach { it.doBecomeInactive() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @CallSuper
    protected fun onStop() {
        if (logging) log("onStop")
        inForeground = false
        controllers.forEach { it.doEnterBackground() }
        presenters.forEach { it.doEnterBackground() }
        val stoppedActivity = stoppedActivityRef?.get() ?: return
        stoppedActivityRef?.clear()
        stoppedActivityRef = null
        stoppedActivity.enterBackground()
    }

    override fun onActivityCreated(activity: android.app.Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: android.app.Activity) {
        val startedActivity = activity as Activity<*, *>
        if (startedActivities.isNotEmpty() || !inForeground) {
            startedActivity.enterForeground()
        }
        startedActivities.add(startedActivity)
    }

    @CallSuper
    override fun onActivityResumed(activity: android.app.Activity) {
        val resumedActivity = activity as Activity<*, *>
        startedActivities.remove(resumedActivity)
        resumedActivityRef = WeakReference(resumedActivity)
    }

    @CallSuper
    override fun onActivityPaused(activity: android.app.Activity) {
        val pausedActivity = activity as Activity<*, *>
        resumedActivityRef?.clear()
        resumedActivityRef = null
        startedActivities.add(pausedActivity)
    }

    override fun onActivityStopped(activity: android.app.Activity) {
        val stoppedActivity = activity as Activity<*, *>
        startedActivities.remove(stoppedActivity)
        stoppedActivityRef = WeakReference(stoppedActivity)
        if (resumedActivityRef?.get() != null) {
            stoppedActivity.enterBackground()
        }
    }

    override fun onActivityDestroyed(activity: android.app.Activity) {}

    override fun onActivitySaveInstanceState(activity: android.app.Activity, bundle: Bundle?) {}

    //endregion

    //region <==========|Controllers|==========>

    protected val controllerProviders: MutableMap<KClass<*>, Provider<Controller>> = mutableMapOf()
    private val controllers: MutableList<Controller> = mutableListOf()

    protected inline fun <reified C : Controller> setControllerProvider(controllerProvider: Provider<C>) {
        @Suppress("UNCHECKED_CAST")
        this.controllerProviders[C::class] = controllerProvider as Provider<Controller>
    }

    fun <P : Controller> controller(controllerClass: KClass<P>): P? {
        @Suppress("UNCHECKED_CAST")
        return controllers.firstOrNull {
            it::class.isSubclassOf(controllerClass)
        } as? P
    }

    fun setUpController(controllerClass: KClass<*>, params: Map<String, Any>) {
        if (controllers.any { it::class.isSubclassOf(controllerClass) }) return
        val controllerProvider = controllerProviders.toList().firstOrNull { it.first == controllerClass }?.second
        val controller = if (controllerProvider != null) controllerProvider.get() else controllerClass.createInstance() as Controller
        controller.application = this
        controllers.add(controller)
        controller.doSetUp(params)
        Interactor.registerGateway(controller)
        if (inForeground) {
            controller.doEnterForeground()
        }
        if (isActive) {
            controller.doBecomeActive()
        }
    }

    fun tearDownController(controllerClass: KClass<*>) {
        val controller = controllers.firstOrNull {
            it::class.isSubclassOf(controllerClass)
        } ?: return
        if (isActive) {
            controller.doBecomeInactive()
        }
        if (inForeground) {
            controller.doEnterBackground()
        }
        Interactor.unregisterGateway(controller)
        controller.doTearDown()
        controllers.remove(controller)
    }

    //endregion

    //region <==========|Presenters|==========>

    protected val presenterProviders: MutableMap<KClass<*>, Provider<Presenter>> = mutableMapOf()
    internal val presenters: MutableList<Presenter> = mutableListOf()

    protected inline fun <reified P : Presenter> setPresenterProvider(presenterProvider: Provider<P>) {
        @Suppress("UNCHECKED_CAST")
        this.presenterProviders[P::class] = presenterProvider as Provider<Presenter>
    }

    fun <P : Presenter> presenter(presenterClass: KClass<P>): P? {
        @Suppress("UNCHECKED_CAST")
        return presenters.firstOrNull {
            it::class.isSubclassOf(presenterClass)
        } as? P
    }

    fun setUpPresenter(presenterClass: KClass<*>, params: Map<String, Any>) {
        if (presenters.any { it::class.isSubclassOf(presenterClass) }) return
        val presenterProvider = presenterProviders.toList().firstOrNull { it.first == presenterClass }?.second
        val presenter = if (presenterProvider != null) presenterProvider.get() else presenterClass.createInstance() as Presenter
        presenters.add(presenter)
        presenter.doSetUp(params)
        if (inForeground) {
            presenter.doEnterForeground()
        }
        if (isActive) {
            presenter.doBecomeActive()
        }
    }

    fun tearDownPresenter(presenterClass: KClass<*>) {
        val presenter = presenters.firstOrNull {
            it::class.isSubclassOf(presenterClass)
        } ?: return
        if (isActive) {
            presenter.doBecomeInactive()
        }
        if (inForeground) {
            presenter.doEnterBackground()
        }
        presenter.doTearDown()
        presenters.remove(presenter)
    }

    //endregion

    //region <==========|Router|==========>

    protected abstract val router: R

    @CallSuper
    override fun route(query: Query): Boolean {
        return when (query.path) {
            Route.Path.PRESENTER_SET_UP -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                setUpPresenter(presenterClass, query.params)
                true
            }
            Route.Path.PRESENTER_TEAR_DOWN -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                tearDownPresenter(presenterClass)
                true
            }
            Route.Path.CONTROLLER_SET_UP -> {
                val controllerClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                setUpController(controllerClass, query.params)
                true
            }
            Route.Path.CONTROLLER_TEAR_DOWN -> {
                val controllerClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                tearDownController(controllerClass)
                true
            }
            Route.Path.ACTIVITY_SHOW -> {
                val currentActivity = resumedActivityRef?.get() ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                val intent = Intent(currentActivity, activityClass)
                currentActivity.startActivity(intent)
                true
            }
            else -> false
        }
    }

    //endregion

}
