/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.router

import kotlin.reflect.KClass

class Route(private val router: Router) {

    object Path {
        const val PRESENTER_SET_UP = "/presenter?set_up"
        const val PRESENTER_TEAR_DOWN = "/presenter?tear_down"
        const val GATEWAY_SET_UP = "/gateway?set_up"
        const val GATEWAY_TEAR_DOWN = "/gateway?tear_down"
        const val ACTIVITY_SHOW = "/activity?show"
        const val FRAGMENT_SHOW_IN_CONTAINER = "/fragment?show_in_container"
        const val FRAGMENT_SHOW_AS_DIALOG = "/fragment?show_as_dialog"
        const val FRAGMENT_HIDE_AS_DIALOG = "/fragment?hide_as_dialog"
    }

    object Param {
        const val COMPONENT_CLASS = "componentClass"
        const val FRAGMENT_CONTAINER = "fragmentContainer"
    }

    private val queries: MutableList<Query> = mutableListOf()

    fun route() {
        router.route(this)
    }

    fun path(path: String, params: Map<String, Any> = mapOf()): Route {
        val query = Query(path, params)
        queries.add(query)
        return this
    }

    fun setUpPresenter(presenterClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.PRESENTER_SET_UP,
                params + mutableMapOf(Route.Param.COMPONENT_CLASS to presenterClass)
        )
        queries.add(query)
        return this
    }

    fun tearDownPresenter(presenterClass: KClass<*>): Route {
        val query = Query(
                Route.Path.PRESENTER_TEAR_DOWN,
                mutableMapOf(Route.Param.COMPONENT_CLASS to presenterClass)
        )
        queries.add(query)
        return this
    }

    fun setUpGateway(gatewayClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.GATEWAY_SET_UP,
                params + mutableMapOf(Route.Param.COMPONENT_CLASS to gatewayClass)
        )
        queries.add(query)
        return this
    }

    fun tearDownGateway(gatewayClass: KClass<*>): Route {
        val query = Query(
                Route.Path.GATEWAY_TEAR_DOWN,
                mutableMapOf(Route.Param.COMPONENT_CLASS to gatewayClass)
        )
        queries.add(query)
        return this
    }

    fun showActivity(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.ACTIVITY_SHOW,
                params + mutableMapOf(Route.Param.COMPONENT_CLASS to activityClass)
        )
        queries.add(query)
        return this
    }

    fun showFragmentInContainer(fragmentClass: KClass<*>, fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.FRAGMENT_SHOW_IN_CONTAINER,
                params + mutableMapOf(
                        Route.Param.COMPONENT_CLASS to fragmentClass,
                        Route.Param.FRAGMENT_CONTAINER to fragmentContainer
                )
        )
        queries.add(query)
        return this
    }

    fun showFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.FRAGMENT_SHOW_AS_DIALOG,
                params + mutableMapOf(
                        Route.Param.COMPONENT_CLASS to fragmentClass
                )
        )
        queries.add(query)
        return this
    }

    fun hideFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.FRAGMENT_HIDE_AS_DIALOG,
                params + mutableMapOf(
                        Route.Param.COMPONENT_CLASS to fragmentClass
                )
        )
        queries.add(query)
        return this
    }

    internal fun nextQuery(): Query? {
        if (queries.isNotEmpty()) {
            queries.removeAt(0)
            return queries.firstOrNull()
        } else {
            throw IllegalStateException("Route is over!")
        }
    }

    internal fun isNotFinished() = queries.isNotEmpty()

    internal fun isFinished() = queries.isEmpty()

    internal fun currentQuery() : Query? = queries.firstOrNull()

}