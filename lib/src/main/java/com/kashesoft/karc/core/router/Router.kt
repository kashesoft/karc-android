/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

import android.os.Handler
import android.os.Looper
import com.kashesoft.karc.utils.Logging
import java.util.*
import kotlin.reflect.KClass

abstract class Router : Logging {

    override val logging = true
    open val loggingLifecycle = false

    private fun log(message: String) {
        if (loggingLifecycle) logVerbose(message)
    }

    private val routables: MutableList<Routable> = mutableListOf()
    private val routes: Queue<Route> = ArrayDeque()

    protected open fun onStartRoute(route: Route) {}

    protected open fun onStopRoute(route: Route) {}

    protected open fun onFinishRoute(route: Route) {}

    fun attachRoutable(routable: Routable) {
        routables.add(routable)
        Handler(Looper.getMainLooper()).post {
            dispatchRoutes()
        }
    }

    fun detachRoutable(routable: Routable) {
        routables.remove(routable)
    }

    @Synchronized
    fun clear(): Router {
        routes.clear()
        return this
    }

    fun path(path: String, params: Map<String, Any> = mapOf()): Route {
        return Route(this).path(path, params)
    }

    fun setUpPresenter(presenterClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).setUpPresenter(presenterClass, params)
    }

    fun tearDownPresenter(presenterClass: KClass<*>): Route {
        return Route(this).tearDownPresenter(presenterClass)
    }

    fun setUpGateway(gatewayClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).setUpGateway(gatewayClass, params)
    }

    fun tearDownGateway(gatewayClass: KClass<*>): Route {
        return Route(this).tearDownGateway(gatewayClass)
    }

    fun startActivity(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).startActivity(activityClass, params)
    }

    fun startActivityNewClear(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).startActivityNewClear(activityClass, params)
    }

    fun finishActivity(activityClass: KClass<*>? = null): Route {
        return Route(this).finishActivity(activityClass)
    }

    fun finishActivityExcept(activityClass: KClass<*>): Route {
        return Route(this).finishActivityExcept(activityClass)
    }

    fun showFragmentInContainer(fragmentClass: KClass<*>, fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentInContainer(fragmentClass, fragmentContainer, params)
    }

    fun showFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentAsDialog(fragmentClass, params)
    }

    fun hideFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).hideFragmentAsDialog(fragmentClass, params)
    }

    @Synchronized
    internal fun route(route: Route) {
        if (route.isFinished()) {
            finishRoute(route)
            return
        }
        startRoute(route)
        var query: Query
        do {
            query = route.currentQuery() ?: break
        } while (
                if (routables.any { it.route(query) }) {
                    route.nextQuery()
                    true
                } else {
                    false
                }
        )
        if (route.isFinished()) {
            finishRoute(route)
        } else {
            stopRoute(route)
        }
    }

    @Synchronized
    internal fun paramsForComponent(componentClass: KClass<*>): Map<String, Any> {
        val query: Query = routes.mapNotNull { it.currentQuery() }.lastOrNull { it.params[Route.Param.COMPONENT_CLASS] == componentClass } ?: return mapOf()
        val params = query.params.toMutableMap()
        params.remove(Route.Param.COMPONENT_CLASS)
        params.remove(Route.Param.FRAGMENT_CONTAINER)
        return params.toMap()
    }

    @Synchronized
    private fun dispatchRoutes() {
        val routes = ArrayDeque(routes)
        this.routes.clear()
        while (routes.size > 0) {
            val route = routes.remove()
            route(route)
        }
    }

    private fun startRoute(route: Route) {
        log("onStartRoute $route")
        onStartRoute(route)
        routes.offer(route)
    }

    private fun stopRoute(route: Route) {
        log("onStopRoute $route")
        onStopRoute(route)
        if (route.timeout > 0) {
            dismissRouteIfNeededAfterTimeout(route)
        }
    }

    private fun finishRoute(route: Route) {
        log("onFinishRoute $route")
        onFinishRoute(route)
        routes.remove(route)
    }

    private fun dismissRouteIfNeededAfterTimeout(route: Route) {
        Handler(Looper.getMainLooper()).postDelayed({
            dismissRouteIfNeeded(route)
        }, route.timeout)
    }

    @Synchronized
    private fun dismissRouteIfNeeded(route: Route) {
        if (routes.contains(route)) {
            finishRoute(route)
        }
    }

}
