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
import com.kashesoft.karc.core.interactor.Gateway
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
    @Synchronized
    protected fun onStart() {
        if (logging) log("onStart")
        inForeground = true

        gateways.toList().forEach {
            it.doEnterForeground()
        }
        presenters.toList().forEach {
            it.doEnterForeground()
        }
    }

    @Synchronized
    private fun onResume() {
        if (logging) log("onResume")
        isActive = true

        gateways.toList().forEach {
            it.doBecomeActive()
        }
        presenters.toList().forEach {
            it.doBecomeActive()
        }
    }

    @Synchronized
    private fun onPause() {
        if (logging) log("onPause")
        isActive = false

        gateways.toList().forEach {
            it.doBecomeInactive()
        }
        presenters.toList().forEach {
            it.doBecomeInactive()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Synchronized
    protected fun onStop() {
        if (logging) log("onStop")
        inForeground = false

        gateways.toList().forEach {
            it.doEnterBackground()
        }
        presenters.toList().forEach {
            it.doEnterBackground()
        }

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

        onResume()
    }

    @CallSuper
    override fun onActivityPaused(activity: android.app.Activity) {
        onPause()

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

    //region <==========|Gateways|==========>

    protected val gatewayProviders: MutableMap<KClass<*>, Provider<Gateway>> = mutableMapOf()
    private val gateways: MutableList<Gateway> = mutableListOf()

    protected inline fun <reified G : Gateway> setGatewayProvider(gatewayProvider: Provider<G>) {
        @Suppress("UNCHECKED_CAST")
        this.gatewayProviders[G::class] = gatewayProvider as Provider<Gateway>
    }

    @Synchronized
    fun <G : Gateway> gateway(gatewayClass: KClass<G>): G? {
        @Suppress("UNCHECKED_CAST")
        return gateways.firstOrNull {
            it::class.isSubclassOf(gatewayClass)
        } as? G
    }

    @Synchronized
    fun setUpGateway(gatewayClass: KClass<*>, params: Map<String, Any>) {
        if (gateways.any { it::class.isSubclassOf(gatewayClass) }) return
        val gatewayProvider = gatewayProviders.toList().firstOrNull { it.first == gatewayClass }?.second
        val gateway = if (gatewayProvider != null) gatewayProvider.get() else gatewayClass.createInstance() as Gateway
        gateways.add(gateway)
        gateway.doSetUp(params)
        if (inForeground) {
            gateway.doEnterForeground()
        }
        if (isActive) {
            gateway.doBecomeActive()
        }
    }

    @Synchronized
    fun tearDownGateway(gatewayClass: KClass<*>) {
        val gateway = gateways.firstOrNull {
            it::class.isSubclassOf(gatewayClass)
        } ?: return
        if (isActive) {
            gateway.doBecomeInactive()
        }
        if (inForeground) {
            gateway.doEnterBackground()
        }
        gateway.doTearDown()
        gateways.remove(gateway)
    }

    //endregion

    //region <==========|Presenters|==========>

    protected val presenterProviders: MutableMap<KClass<*>, Provider<Presenter>> = mutableMapOf()
    private val presenters: MutableList<Presenter> = mutableListOf()

    @Synchronized
    internal fun getPresenters(): List<Presenter> {
        return presenters
    }

    protected inline fun <reified P : Presenter> setPresenterProvider(presenterProvider: Provider<P>) {
        @Suppress("UNCHECKED_CAST")
        this.presenterProviders[P::class] = presenterProvider as Provider<Presenter>
    }

    @Synchronized
    fun <P : Presenter> presenter(presenterClass: KClass<P>): P? {
        @Suppress("UNCHECKED_CAST")
        return presenters.firstOrNull {
            it::class.isSubclassOf(presenterClass)
        } as? P
    }

    @Synchronized
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

    @Synchronized
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
            Route.Path.GATEWAY_SET_UP -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                setUpGateway(gatewayClass, query.params)
                true
            }
            Route.Path.GATEWAY_TEAR_DOWN -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                tearDownGateway(gatewayClass)
                true
            }
            Route.Path.ACTIVITY_SHOW -> {
                val currentActivity = resumedActivityRef?.get() ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                val intent = Intent(currentActivity, activityClass)
                currentActivity.startActivity(intent)
                currentActivity.detachCompanionRouter()
                true
            }
            else -> false
        }
    }

    //endregion

}
