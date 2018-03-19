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
        const val DEFAULT_ROUTED_QUERY_DISMISS_DELAY = 2000L
        const val DEFAULT_REMAINING_ROUTE_DISMISS_DELAY = 2000L
    }

    private val routables: MutableList<Routable> = mutableListOf()
    private val remainingRoutes: Queue<Route> = ArrayDeque()
    private var routedQueries = mutableListOf<Query>()

    fun attachRoutable(routable: Routable) {
        routables.add(routable)
        dispatchRoutes()
    }

    fun detachRoutable(routable: Routable) {
        routables.remove(routable)
    }

    @Synchronized
    fun clear() {
        remainingRoutes.clear()
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

    fun showActivity(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showActivity(activityClass, params)
    }

    fun showFragmentInContainer(fragmentClass: KClass<*>, fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        return Route(this).showFragmentInContainer(fragmentClass, fragmentContainer, params)
    }

    @Synchronized
    internal fun route(route: Route) {
        var query = route.currentQuery() ?: return
        while (routables.any { it.route(query) }) {
            routedQueries.add(query)
            dismissRoutedQueryIfNeededAfterDelay(query, DEFAULT_ROUTED_QUERY_DISMISS_DELAY)
            query = route.nextQuery() ?: return
        }
        if (route.isNotFinished()) {
            remainingRoutes.offer(route)
            dismissRemainingRouteIfNeededAfterDelay(route, DEFAULT_REMAINING_ROUTE_DISMISS_DELAY)
        }
    }

    @Synchronized
    internal fun paramsForComponent(componentClass: KClass<*>): Map<String, Any> {
        val query: Query = routedQueries.firstOrNull { it.params[Route.Param.COMPONENT_CLASS] == componentClass } ?: return mapOf()
        routedQueries.remove(query)
        val params = query.params.toMutableMap()
        params.remove(Route.Param.COMPONENT_CLASS)
        params.remove(Route.Param.FRAGMENT_CONTAINER)
        return params.toMap()
    }

    @Synchronized
    private fun dispatchRoutes() {
        val routes = ArrayDeque(remainingRoutes)
        remainingRoutes.clear()
        while (routes.size > 0) {
            val route = routes.remove()
            route(route)
        }
    }

    private fun dismissRoutedQueryIfNeededAfterDelay(routedQuery: Query, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            dismissRoutedQueryIfNeeded(routedQuery)
        }, delay)
    }

    @Synchronized
    private fun dismissRoutedQueryIfNeeded(routedQuery: Query) {
        if (routedQueries.contains(routedQuery)) {
            routedQueries.remove(routedQuery)
        }
    }

    private fun dismissRemainingRouteIfNeededAfterDelay(remainingRoute: Route, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            dismissRemainingRouteIfNeeded(remainingRoute)
        }, delay)
    }

    @Synchronized
    private fun dismissRemainingRouteIfNeeded(remainingRoute: Route) {
        if (remainingRoutes.contains(remainingRoute)) {
            remainingRoutes.remove(remainingRoute)
        }
    }

}
