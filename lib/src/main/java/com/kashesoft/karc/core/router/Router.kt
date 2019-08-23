/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

import android.os.Handler
import android.os.Looper
import com.kashesoft.karc.core.Mode
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

    fun setUpPresenter(componentClass: KClass<*>, componentTag: String = "default", componentMode: Mode = Mode.UI_SYNC, params: Map<String, Any> = mapOf()): Route {
        return Route(this).setUpPresenter(componentClass, componentTag, componentMode, params)
    }

    fun tearDownPresenter(componentClass: KClass<*>, componentTag: String = "default"): Route {
        return Route(this).tearDownPresenter(componentClass, componentTag)
    }

    fun setUpGateway(componentClass: KClass<*>, componentTag: String = "default", componentMode: Mode = Mode.UI_SYNC, params: Map<String, Any> = mapOf()): Route {
        return Route(this).setUpGateway(componentClass, componentTag, componentMode, params)
    }

    fun tearDownGateway(componentClass: KClass<*>, componentTag: String = "default"): Route {
        return Route(this).tearDownGateway(componentClass, componentTag)
    }

    fun startActivity(componentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).startActivity(componentClass, params)
    }

    fun startActivityNewClear(componentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).startActivityNewClear(componentClass, params)
    }

    fun finishActivity(componentClass: KClass<*>? = null): Route {
        return Route(this).finishActivity(componentClass)
    }

    fun finishActivityExcept(componentClass: KClass<*>): Route {
        return Route(this).finishActivityExcept(componentClass)
    }

    fun showFragmentInContainer(componentClass: KClass<*>, componentTag: String = "default", fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentInContainer(componentClass, componentTag, fragmentContainer, params)
    }

    fun showFragmentAsDialog(componentClass: KClass<*>, componentTag: String = "default", params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentAsDialog(componentClass, componentTag, params)
    }

    fun hideFragmentAsDialog(componentClass: KClass<*>, componentTag: String = "default", params: Map<String, Any> = mapOf()): Route {
        return Route(this).hideFragmentAsDialog(componentClass, componentTag, params)
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
    internal fun paramsForComponent(componentClass: KClass<*>, componentTag: String): Map<String, Any> {
        val query: Query = routes.mapNotNull { it.currentQuery() }.lastOrNull { it.params[Route.Param.COMPONENT_CLASS] == componentClass && it.params[Route.Param.COMPONENT_TAG] == componentTag } ?: return mapOf()
        val params = query.params.toMutableMap()
        params.remove(Route.Param.COMPONENT_CLASS)
        params.remove(Route.Param.COMPONENT_TAG)
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
