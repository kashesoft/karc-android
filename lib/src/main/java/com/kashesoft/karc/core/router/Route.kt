/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

import kotlin.reflect.KClass

class Route(private val router: Router) {

    object Path {
        const val PRESENTER_SET_UP = "/presenter?set_up"
        const val PRESENTER_TEAR_DOWN = "/presenter?tear_down"
        const val GATEWAY_SET_UP = "/gateway?set_up"
        const val GATEWAY_TEAR_DOWN = "/gateway?tear_down"
        const val ACTIVITY_START = "/activity?start"
        const val ACTIVITY_START_NEW_CLEAR = "/activity?start_new_clear"
        const val ACTIVITY_FINISH = "/activity?finish"
        const val ACTIVITY_FINISH_EXCEPT = "/activity?finish_except"
        const val FRAGMENT_SHOW_IN_CONTAINER = "/fragment?show_in_container"
        const val FRAGMENT_SHOW_AS_DIALOG = "/fragment?show_as_dialog"
        const val FRAGMENT_HIDE_AS_DIALOG = "/fragment?hide_as_dialog"
    }

    object Param {
        const val COMPONENT_CLASS = "componentClass"
        const val FRAGMENT_CONTAINER = "fragmentContainer"
    }

    private val queries: MutableList<Query> = mutableListOf()
    private val routedQueries: MutableList<Query> = mutableListOf()

    override fun toString(): String {
        return "${routedQueries.joinToString(" +++ ", transform = { "${it.path} | ${it.params}" })} >>> ${queries.joinToString(" +++ ", transform = { "${it.path} | ${it.params}" })}"
    }

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
                Path.PRESENTER_SET_UP,
                params + mapOf(Param.COMPONENT_CLASS to presenterClass)
        )
        queries.add(query)
        return this
    }

    fun tearDownPresenter(presenterClass: KClass<*>): Route {
        val query = Query(
                Path.PRESENTER_TEAR_DOWN,
                mapOf(Param.COMPONENT_CLASS to presenterClass)
        )
        queries.add(query)
        return this
    }

    fun setUpGateway(gatewayClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.GATEWAY_SET_UP,
                params + mapOf(Param.COMPONENT_CLASS to gatewayClass)
        )
        queries.add(query)
        return this
    }

    fun tearDownGateway(gatewayClass: KClass<*>): Route {
        val query = Query(
                Path.GATEWAY_TEAR_DOWN,
                mapOf(Param.COMPONENT_CLASS to gatewayClass)
        )
        queries.add(query)
        return this
    }

    fun startActivity(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.ACTIVITY_START,
                params + mapOf(Param.COMPONENT_CLASS to activityClass)
        )
        queries.add(query)
        return this
    }

    fun startActivityNewClear(activityClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.ACTIVITY_START_NEW_CLEAR,
                params + mapOf(Param.COMPONENT_CLASS to activityClass)
        )
        queries.add(query)
        return this
    }

    fun finishActivity(activityClass: KClass<*>? = null): Route {
        val query = Query(
                Path.ACTIVITY_FINISH,
                if (activityClass != null) mapOf(Param.COMPONENT_CLASS to activityClass) else mapOf()
        )
        queries.add(query)
        return this
    }

    fun finishActivityExcept(activityClass: KClass<*>): Route {
        val query = Query(
                Path.ACTIVITY_FINISH_EXCEPT,
                mapOf(Param.COMPONENT_CLASS to activityClass)
        )
        queries.add(query)
        return this
    }

    fun showFragmentInContainer(fragmentClass: KClass<*>, fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.FRAGMENT_SHOW_IN_CONTAINER,
                params + mapOf(
                        Param.COMPONENT_CLASS to fragmentClass,
                        Param.FRAGMENT_CONTAINER to fragmentContainer
                )
        )
        queries.add(query)
        return this
    }

    fun showFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.FRAGMENT_SHOW_AS_DIALOG,
                params + mapOf(
                        Param.COMPONENT_CLASS to fragmentClass
                )
        )
        queries.add(query)
        return this
    }

    fun hideFragmentAsDialog(fragmentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.FRAGMENT_HIDE_AS_DIALOG,
                params + mapOf(
                        Param.COMPONENT_CLASS to fragmentClass
                )
        )
        queries.add(query)
        return this
    }

    internal fun nextQuery(): Query? {
        if (queries.isNotEmpty()) {
            val routedQuery = queries.removeAt(0)
            routedQueries.add(routedQuery)
            return queries.firstOrNull()
        } else {
            throw IllegalStateException("Route is over!")
        }
    }

    internal fun isNotFinished() = queries.isNotEmpty()

    internal fun isFinished() = queries.isEmpty()

    internal fun currentQuery() : Query? = queries.firstOrNull()

    var timeout: Long = 0
        private set

    fun withTimeout(timeout: Long): Route {
        this.timeout = timeout
        return this
    }

}
