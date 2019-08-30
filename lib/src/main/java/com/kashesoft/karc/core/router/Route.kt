/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

import com.kashesoft.karc.core.Mode
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
        const val COMPONENT_TAG = "componentTag"
        const val COMPONENT_MODE = "componentMode"
        const val FRAGMENT_CONTAINER = "fragmentContainer"
    }

    private val queries: MutableList<Query> = mutableListOf()
    private val routedQueries: MutableList<Query> = mutableListOf()

    override fun toString(): String {
        return "${routedQueries.joinToString(" +++ ", transform = { "${it.path} | ${it.params} | ${it.pending}" })} >>> ${queries.joinToString(" +++ ", transform = { "${it.path} | ${it.params} | ${it.pending}" })}"
    }

    fun route() {
        router.route(this)
    }

    fun path(path: String, params: Map<String, Any> = mapOf()): Route {
        val query = Query(path, params)
        queries.add(query)
        return this
    }

    fun setUpPresenter(componentClass: KClass<*>, componentTag: String = "default", componentMode: Mode = Mode.UI_SYNC, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.PRESENTER_SET_UP,
                params + mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to componentTag, Param.COMPONENT_MODE to componentMode)
        )
        queries.add(query)
        return this
    }

    fun tearDownPresenter(componentClass: KClass<*>, componentTag: String = "default"): Route {
        val query = Query(
                Path.PRESENTER_TEAR_DOWN,
                mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to componentTag)
        )
        queries.add(query)
        return this
    }

    fun setUpGateway(componentClass: KClass<*>, componentTag: String = "default", componentMode: Mode = Mode.UI_SYNC, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.GATEWAY_SET_UP,
                params + mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to componentTag, Param.COMPONENT_MODE to componentMode)
        )
        queries.add(query)
        return this
    }

    fun tearDownGateway(componentClass: KClass<*>, componentTag: String = "default"): Route {
        val query = Query(
                Path.GATEWAY_TEAR_DOWN,
                mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to componentTag)
        )
        queries.add(query)
        return this
    }

    fun startActivity(componentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.ACTIVITY_START,
                params + mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to "default"),
                pending = true
        )
        queries.add(query)
        return this
    }

    fun startActivityNewClear(componentClass: KClass<*>, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.ACTIVITY_START_NEW_CLEAR,
                params + mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to "default"),
                pending = true
        )
        queries.add(query)
        return this
    }

    fun finishActivity(componentClass: KClass<*>? = null): Route {
        val query = Query(
                Path.ACTIVITY_FINISH,
                if (componentClass != null) mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to "default") else mapOf()
        )
        queries.add(query)
        return this
    }

    fun finishActivityExcept(componentClass: KClass<*>): Route {
        val query = Query(
                Path.ACTIVITY_FINISH_EXCEPT,
                mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to "default")
        )
        queries.add(query)
        return this
    }

    fun showFragmentInContainer(componentClass: KClass<*>, componentTag: String = "default", fragmentContainer: Int, params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.FRAGMENT_SHOW_IN_CONTAINER,
                params + mapOf(
                        Param.COMPONENT_CLASS to componentClass,
                        Param.COMPONENT_TAG to componentTag,
                        Param.FRAGMENT_CONTAINER to fragmentContainer
                )
        )
        queries.add(query)
        return this
    }

    fun showFragmentAsDialog(componentClass: KClass<*>, componentTag: String = "default", params: Map<String, Any> = mapOf()): Route {
        val query = Query(
                Path.FRAGMENT_SHOW_AS_DIALOG,
                params + mapOf(
                        Param.COMPONENT_CLASS to componentClass,
                        Param.COMPONENT_TAG to componentTag
                )
        )
        queries.add(query)
        return this
    }

    fun hideFragmentAsDialog(componentClass: KClass<*>, componentTag: String = "default"): Route {
        val query = Query(
                Path.FRAGMENT_HIDE_AS_DIALOG,
                mapOf(Param.COMPONENT_CLASS to componentClass, Param.COMPONENT_TAG to componentTag)
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

    internal fun isNotFinished() = !isFinished()

    internal fun isFinished() = queries.isEmpty() && routedQueries.none { it.pending }

    internal fun currentQuery(): Query? = queries.firstOrNull()

    internal fun lastRoutedQuery(): Query? = routedQueries.lastOrNull()

    var timeout: Long = 0
        private set

    fun withTimeout(timeout: Long): Route {
        this.timeout = timeout
        return this
    }

    internal fun isPending(): Boolean {
        return lastRoutedQuery()?.pending == true
    }

}
