/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import com.kashesoft.karc.core.gateway.Gateway
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.core.router.Router
import com.kashesoft.karc.utils.*
import java.lang.ref.WeakReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private typealias KarcActivity = com.kashesoft.karc.app.Activity<*>
private typealias KarcApplication = com.kashesoft.karc.app.Application<*>

inline val <reified A : com.kashesoft.karc.app.Application<R>, R : Router> KClass<A>.get: A
    get() = com.kashesoft.karc.app.Application.instance as A

inline val <reified R : Router> KClass<R>.get: R
    get() = com.kashesoft.karc.app.Application.instance.router as R

inline val <reified G : Gateway> KClass<G>.get: G
    get() = com.kashesoft.karc.app.Application.instance.gateway(this)!!

inline val <reified G : Gateway> KClass<G>.getOrNull: G?
    get() = com.kashesoft.karc.app.Application.instance.gateway(this)

inline val <reified P : Presenter> KClass<P>.get: P
    get() = com.kashesoft.karc.app.Application.instance.presenter(this)!!

inline val <reified P : Presenter> KClass<P>.getOrNull: P?
    get() = com.kashesoft.karc.app.Application.instance.presenter(this)

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

    private var resumedActivityRef: WeakReference<Activity>? = null
    private var startedActivities: MutableSet<Activity> = mutableSetOf()
    private var stoppedActivityRef: WeakReference<Activity>? = null
    private var inForeground = false
    private var isActive = false

    val resumedActivity: Activity?
        get() = resumedActivityRef?.get()

    @CallSuper
    override fun onCreate() {
        log("onCreate")
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)
        router.attachRoutable(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @Synchronized
    protected fun onStart() {
        log("onStart")
        inForeground = true

        gateways.toList().forEach {
            it.doEnterForeground()
        }
        presenters.toList().forEach {
            it.doEnterForeground()
        }
        onEnterForeground()
    }

    @Synchronized
    private fun onResume() {
        log("onResume")
        isActive = true

        gateways.toList().forEach {
            it.doBecomeActive()
        }
        presenters.toList().forEach {
            it.doBecomeActive()
        }
        onBecomeActive()
    }

    @Synchronized
    private fun onPause() {
        log("onPause")
        isActive = false

        onBecomeInactive()
        presenters.toList().forEach {
            it.doBecomeInactive()
        }
        gateways.toList().forEach {
            it.doBecomeInactive()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Synchronized
    protected fun onStop() {
        log("onStop")
        inForeground = false

        onEnterBackground()
        presenters.toList().forEach {
            it.doEnterBackground()
        }
        gateways.toList().forEach {
            it.doEnterBackground()
        }

        val stoppedActivity = stoppedActivityRef?.get() ?: return
        stoppedActivityRef?.clear()
        stoppedActivityRef = null
        (stoppedActivity as? KarcActivity)?.enterBackground()
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities.isNotEmpty() || !inForeground) {
            (activity as? KarcActivity)?.enterForeground()
        }
        startedActivities.add(activity)
    }

    @CallSuper
    override fun onActivityResumed(activity: Activity) {
        startedActivities.remove(activity)
        resumedActivityRef = WeakReference(activity)

        onResume()
    }

    @CallSuper
    override fun onActivityPaused(activity: Activity) {
        onPause()

        resumedActivityRef?.clear()
        resumedActivityRef = null
        startedActivities.add(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(activity)
        stoppedActivityRef = WeakReference(activity)
        if (resumedActivityRef?.get() != null) {
            (activity as? KarcActivity)?.enterBackground()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {}

    open fun onEnterForeground() {}

    open fun onBecomeActive() {}

    open fun onBecomeInactive() {}

    open fun onEnterBackground() {}

    //endregion

    //region <==========|Gateways|==========>

    protected val gatewayProviders: MutableMap<KClass<*>, Provider<Gateway>> = mutableMapOf()
    private val gateways: MutableList<Gateway> = mutableListOf()

    fun getGateways(): List<Gateway> = gateways.toList()

    protected inline fun <reified G : Gateway> setGatewayProvider(gatewayProvider: Provider<G>) {
        @Suppress("UNCHECKED_CAST")
        this.gatewayProviders[G::class] = gatewayProvider as Provider<Gateway>
    }

    @Synchronized
    inline fun <reified G : Gateway> gateway(gatewayClass: KClass<G>): G? {
        return getGateways().firstOrNull {
            gatewayClass.java.isAssignableFrom(it::class.java)
        } as? G
    }

    @Synchronized
    fun setUpGateway(gatewayClass: KClass<*>, params: Map<String, Any>) {
        if (gateways.any { gatewayClass.java.isAssignableFrom(it::class.java) }) return
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
            gatewayClass.java.isAssignableFrom(it::class.java)
        } ?: return
        if (gateway.isActive) {
            gateway.doBecomeInactive()
        }
        if (gateway.inForeground) {
            gateway.doEnterBackground()
        }
        gateway.doTearDown()
        gateways.remove(gateway)
    }

    //endregion

    //region <==========|Presenters|==========>

    protected val presenterProviders: MutableMap<KClass<*>, Provider<Presenter>> = mutableMapOf()
    private val presenters: MutableList<Presenter> = mutableListOf()
    private val presentableForPresenterClassesMap: MutableMap<Presentable, MutableSet<KClass<*>>> = mutableMapOf()

    fun getPresenters(): List<Presenter> = presenters.toList()

    @Synchronized
    internal fun attachPresentableToPresenterWithClass(presentable: Presentable, presenterClass: KClass<*>) {
        if (presentableForPresenterClassesMap[presentable] == null) {
            presentableForPresenterClassesMap[presentable] = mutableSetOf()
        }
        presentableForPresenterClassesMap[presentable]?.add(presenterClass)

        val presenter: Presenter = presenters.firstOrNull {
            presenterClass.java.isAssignableFrom(it::class.java)
        } ?: return
        presenter.attachPresentable(presentable)
    }

    @Synchronized
    internal fun detachPresentableFromPresenterWithClass(presentable: Presentable, presenterClass: KClass<*>) {
        presentableForPresenterClassesMap[presentable]?.remove(presenterClass)
        if (presentableForPresenterClassesMap[presentable]?.isEmpty() == true) {
            presentableForPresenterClassesMap.remove(presentable)
        }

        val presenter: Presenter = presenters.firstOrNull {
            presenterClass.java.isAssignableFrom(it::class.java)
        } ?: return
        presenter.detachPresentable(presentable)
    }

    protected inline fun <reified P : Presenter> setPresenterProvider(presenterProvider: Provider<P>) {
        @Suppress("UNCHECKED_CAST")
        this.presenterProviders[P::class] = presenterProvider as Provider<Presenter>
    }

    @Synchronized
    inline fun <reified P : Presenter> presenter(presenterClass: KClass<P>): P? {
        return getPresenters().firstOrNull {
            presenterClass.java.isAssignableFrom(it::class.java)
        } as? P
    }

    @Synchronized
    fun setUpPresenter(presenterClass: KClass<*>, params: Map<String, Any>) {
        if (presenters.any { presenterClass.java.isAssignableFrom(it::class.java) }) return
        val presenterProvider = presenterProviders.toList().firstOrNull { it.first == presenterClass }?.second
        val presenter = if (presenterProvider != null) presenterProvider.get() else presenterClass.createInstance() as Presenter
        presenters.add(presenter)
        attachPresentablesForPresenter(presenter)
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
            presenterClass.java.isAssignableFrom(it::class.java)
        } ?: return
        if (presenter.isActive) {
            presenter.doBecomeInactive()
        }
        if (presenter.inForeground) {
            presenter.doEnterBackground()
        }
        presenter.doTearDown()
        presenters.remove(presenter)
    }

    private fun attachPresentablesForPresenter(presenter: Presenter) {
        presentableForPresenterClassesMap
                .filter { (_, presenterClasses) -> presenterClasses.any { it == presenter::class } }
                .map { it.key }
                .forEach { presenter.attachPresentable(it) }
    }

    //endregion

    //region <==========|Router|==========>

    abstract val router: R

    @CallSuper
    override fun route(query: Query): Boolean {
        return when (query.path) {
            Route.Path.PRESENTER_SET_UP -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Set up ${presenterClass.simpleName}")
                setUpPresenter(presenterClass, query.params)
                true
            }
            Route.Path.PRESENTER_TEAR_DOWN -> {
                val presenterClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Tear down ${presenterClass.simpleName}")
                tearDownPresenter(presenterClass)
                true
            }
            Route.Path.GATEWAY_SET_UP -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Set up ${gatewayClass.simpleName}")
                setUpGateway(gatewayClass, query.params)
                true
            }
            Route.Path.GATEWAY_TEAR_DOWN -> {
                val gatewayClass: KClass<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>)
                log("Tear down ${gatewayClass.simpleName}")
                tearDownGateway(gatewayClass)
                true
            }
            Route.Path.ACTIVITY_START -> {
                val currentActivity = resumedActivityRef?.get() ?: return false
                val activityClass: Class<*> = (query.params[Route.Param.COMPONENT_CLASS] as KClass<*>).java
                if (currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                val intent = Intent(currentActivity, activityClass)
                log("Start ${activityClass.kotlin.simpleName} from ${currentActivity::class.simpleName}")
                currentActivity.startActivity(intent)
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            Route.Path.ACTIVITY_START_NEW_CLEAR -> {
                val currentActivity = resumedActivityRef?.get() ?: return false
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
                val currentActivity = resumedActivityRef?.get() ?: return false
                val activityClass: Class<*>? = (query.params[Route.Param.COMPONENT_CLASS] as? KClass<*>)?.java
                if (activityClass != null && !currentActivity::class.javaObjectType.isAssignableFrom(activityClass)) return true
                log("Finish ${currentActivity::class.simpleName}")
                currentActivity.finish()
                (currentActivity as? KarcActivity)?.detachCompanionRouter()
                true
            }
            Route.Path.ACTIVITY_FINISH_EXCEPT -> {
                val currentActivity = resumedActivityRef?.get() ?: return false
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

    protected fun activateObjectProfiler() {
        ObjectProfiler.activate()
    }

    protected fun deactivateObjectProfiler() {
        ObjectProfiler.deactivate()
    }

    protected open fun onObjectProfilerEvent(event: ProfileObjectEvent) {}

    //endregion

}
