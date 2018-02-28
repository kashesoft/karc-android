/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.router

import kotlin.reflect.KClass

class Route(private val router: Router) {

    object Path {
        const val PRESENTER_SET_UP = "/presenter?set_up"
        const val PRESENTER_TEAR_DOWN = "/presenter?tear_down"
        const val CONTROLLER_SET_UP = "/controller?set_up"
        const val CONTROLLER_TEAR_DOWN = "/controller?tear_down"
        const val ACTIVITY_SHOW = "/activity?show"
        const val FRAGMENT_SHOW_IN_CONTAINER = "/fragment?show_in_container"
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

    fun setUpController(controllerClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Route.Path.CONTROLLER_SET_UP,
                params + mutableMapOf(Route.Param.COMPONENT_CLASS to controllerClass)
        )
        queries.add(query)
        return this
    }

    fun tearDownController(controllerClass: KClass<*>): Route {
        val query = Query(
                Route.Path.CONTROLLER_TEAR_DOWN,
                mutableMapOf(Route.Param.COMPONENT_CLASS to controllerClass)
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

    internal fun next(): Boolean {
        if (queries.isNotEmpty()) {
            queries.removeAt(0)
            return true
        } else {
            throw IllegalStateException("Route is over!")
        }
    }

    internal fun isNotFinished() = queries.isNotEmpty()

    internal fun isFinished() = queries.isEmpty()

    internal fun currentQuery() : Query? = queries.firstOrNull()

}