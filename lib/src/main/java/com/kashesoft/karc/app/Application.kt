/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.kashesoft.karc.core.Component
import com.kashesoft.karc.core.Core
import com.kashesoft.karc.core.Mode
import com.kashesoft.karc.core.State
import com.kashesoft.karc.core.gateway.Gateway
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.core.router.Router
import com.kashesoft.karc.utils.*
import kotlin.reflect.KClass

private typealias KarcActivity = com.kashesoft.karc.app.Activity<*>
private typealias KarcApplication = com.kashesoft.karc.app.Application<*>

inline val <reified A : com.kashesoft.karc.app.Application<R>, R : Router> KClass<A>.get: A
    get() = com.kashesoft.karc.app.Application.instance as A

inline val <reified R : Router> KClass<R>.get: R
    get() = com.kashesoft.karc.app.Application.instance.router as R

inline val <reified G : Gateway> KClass<G>.get: G
    get() = Core.component(this)!! as G

inline val <reified G : Gateway> KClass<G>.getOrNull: G?
    get() = Core.component(this) as? G

inline val <reified P : Presenter> KClass<P>.get: P
    get() = Core.component(this)!! as P

inline val <reified P : Presenter> KClass<P>.getOrNull: P?
    get() = Core.component(this) as? P

inline fun <reified C : Component, I : C> KClass<C>.setProvider(componentProvider: Provider<I>) {
    Core.setComponentProvider(componentProvider as Provider<Component>, this)
}

abstract class Application<out R : Router> : Application(), Logging,
        LifecycleObserver, Application.ActivityLifecycleCallbacks, Routable {

    companion object {
        val instance: KarcApplication by lazy { instanceProvider() }
        private lateinit var instanceProvider: () -> KarcApplication
    }

    init {
        instanceProvider = { this }
    }

    override val logging = true
    open val loggingLifecycle = false

    private fun log(message: String) {
        if (loggingLifecycle) logVerbose(":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    var resumedActivity: Activity? = null
        private set

    private var startedActivities: MutableSet<Activity> = mutableSetOf()

    @CallSuper
    override fun onCreate() {
        log("onCreate")
        super.onCreate()
        Core.setState(State.BACKGROUND)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)
        router.attachRoutable(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @Synchronized
    protected fun onStart() {
        log("onStart")
        Core.setState(State.INACTIVE)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @Synchronized
    protected fun onResume() {
        log("onResume")
        Core.setState(State.ACTIVE)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @Synchronized
    protected fun onPause() {
        log("onPause")
        Core.setState(State.INACTIVE)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Synchronized
    protected fun onStop() {
        log("onStop")
        Core.setState(State.BACKGROUND)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        startedActivities.add(activity)
    }

    @CallSuper
    override fun onActivityResumed(activity: Activity) {
        startedActivities.remove(activity)
        resumedActivity = activity
    }

    @CallSuper
    override fun onActivityPaused(activity: Activity) {
        resumedActivity = null
        startedActivities.add(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {}

    //endregion

    //region <==========|Router|==========>

    abstract val router: R

    @CallSuper
    override fun route(query: Query): Boolean {
        return when (query.path) {
            Route.Path.PRESENTER_SET_UP -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Set up ${presenterClass.simpleName}")
                Core.setUpComponent(presenterClass, query.params, Mode.IO_ASYNC, true)
                true
            }
            Route.Path.PRESENTER_TEAR_DOWN -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Tear down ${presenterClass.simpleName}")
                Core.tearDownComponent(presenterClass)
                true
            }
            Route.Path.GATEWAY_SET_UP -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Set up ${gatewayClass.simpleName}")
                Core.setUpComponent(gatewayClass, query.params, Mode.IO_ASYNC, true)
                true
            }
            Route.Path.GATEWAY_TEAR_DOWN -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Tear down ${gatewayClass.simpleName}")
                Core.tearDownComponent(gatewayClass)
                true
            }
            Route.Path.ACTIVITY_START -> {
                val currentActivity = resumedActivity ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                val intent = Intent(currentActivity, activityClass)
                log("Start ${activityClass.kotlin.simpleName} from ${currentActivity::class.simpleName}")
                currentActivity.startActivity(intent)
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            Route.Path.ACTIVITY_START_NEW_CLEAR -> {
                val currentActivity = resumedActivity ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                val intent = Intent(currentActivity, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                log("Start ${activityClass.kotlin.simpleName} from ${currentActivity::class.simpleName}")
                currentActivity.startActivity(intent)
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            Route.Path.ACTIVITY_FINISH -> {
                val currentActivity = resumedActivity ?: return false
                val activityClass: Class<*>? = (query.params[Route.Param.COMPONENT_CLASS] as? KClass<*>)?.java
                if (activityClass != null && !currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                log("Finish ${currentActivity::class.simpleName}")
                currentActivity.finish()
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            Route.Path.ACTIVITY_FINISH_EXCEPT -> {
                val currentActivity = resumedActivity ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                log("Finish ${currentActivity::class.simpleName} except ${activityClass.kotlin.simpleName}")
                currentActivity.finish()
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            else -> false
        }
    }

    //endregion

    //region <==========|Profiling|==========>

    internal var autoObjectProfiling = false

    init {
        MethodProfiler.eventListener = { event ->
            onMethodProfilerEvent(event)
        }
        ObjectProfiler.eventListener = { event ->
            onObjectProfilerEvent(event)
        }
    }

    protected fun configureMethodProfiler(
            sampleStepMillis: Long = MethodProfiler.DEFAULT_SAMPLE_STEP_TIME,
            defaultBlockingMaxTime: Long = MethodProfiler.DEFAULT_BLOCKING_MAX_TIME,
            defaultRecursionMaxDepth: Long = MethodProfiler.DEFAULT_RECURSION_MAX_DEPTH
    ) {
        MethodProfiler.sampleStepTime = sampleStepMillis
        MethodProfiler.defaultBlockingMaxTime = defaultBlockingMaxTime
        MethodProfiler.defaultRecursionMaxDepth = defaultRecursionMaxDepth
    }

    protected fun activateMethodProfiler() {
        MethodProfiler.activate()
    }

    protected fun deactivateMethodProfiler() {
        MethodProfiler.deactivate()
    }

    protected open fun onMethodProfilerEvent(event: ProfileMethodEvent) {}

    protected fun configureObjectProfiler(
            sampleStepMillis: Long = ObjectProfiler.DEFAULT_SAMPLE_STEP_TIME
    ) {
        ObjectProfiler.sampleStepTime = sampleStepMillis
    }

    protected fun activateObjectProfiler(autoProfiling: Boolean = false) {
        autoObjectProfiling = autoProfiling
        ObjectProfiler.activate()
    }

    protected fun deactivateObjectProfiler() {
        ObjectProfiler.deactivate()
    }

    protected open fun onObjectProfilerEvent(event: ProfileObjectEvent) {}

    //endregion

}
