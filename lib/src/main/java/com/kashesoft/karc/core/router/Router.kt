/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.router

import android.os.Handler
import android.os.Looper
import com.kashesoft.karc.utils.Logging
import java.util.*
import kotlin.reflect.KClass

open class Router : Logging {

    companion object {
        const val DEFAULT_ROUTE_DISMISS_DELAY = 2000L
    }

    private val routables: MutableList<Routable> = mutableListOf()
    private val routes: Queue<Route> = ArrayDeque()
    private var currentQueries = mutableListOf<Query>()

    fun attachRoutable(routable: Routable) {
        routables.add(routable)
        dispatchRoutes()
    }

    fun detachRoutable(routable: Routable) {
        routables.remove(routable)
    }

    @Synchronized
    fun clear() {
        routes.clear()
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

    fun setUpController(controllerClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).setUpController(controllerClass, params)
    }

    fun tearDownController(controllerClass: KClass<*>): Route {
        return Route(this).tearDownController(controllerClass)
    }

    fun showActivity(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showActivity(activityClass, params)
    }

    fun showFragmentInContainer(fragmentClass: KClass<*>, fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentInContainer(fragmentClass, fragmentContainer, params)
    }

    @Synchronized
    internal fun route(route: Route) {
        val query = route.currentQuery() ?: return
        while (routables.any { it.route(query) }) {
            route.next()
            currentQueries.add(query)
        }
        if (route.isNotFinished()) {
            routes.offer(route)
            dismissRouteIfNeededAfterDelay(route, DEFAULT_ROUTE_DISMISS_DELAY)
        }
    }

    @Synchronized
    internal fun paramsForComponent(componentClass: KClass<*>): Map<String, Any> {
        val query: Query = currentQueries.firstOrNull { it.params[Route.Param.COMPONENT_CLASS] == componentClass } ?: return mapOf()
        currentQueries.remove(query)
        val params = query.params.toMutableMap()
        params.remove(Route.Param.COMPONENT_CLASS)
        params.remove(Route.Param.FRAGMENT_CONTAINER)
        return params.toMap()
    }

    @Synchronized
    private fun dispatchRoutes() {
        val remainingRoutes = ArrayDeque(routes)
        routes.clear()
        while (remainingRoutes.size > 0) {
            val route = remainingRoutes.remove()
            route(route)
        }
    }

    private fun dismissRouteIfNeededAfterDelay(route: Route, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            dismissRouteIfNeeded(route)
        }, delay)
    }

    @Synchronized
    private fun dismissRouteIfNeeded(route: Route) {
        if (routes.contains(route)) {
            routes.remove(route)
        }
    }

}
