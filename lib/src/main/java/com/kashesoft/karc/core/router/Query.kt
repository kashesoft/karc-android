/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

data class Query(
        val path: String,
        val params: Map<String, Any> = mapOf(),
        var pending: Boolean = false
) {

    fun aquireParams(): Map<String, Any> {
        pending = false
        val params = params.toMutableMap()
        params.remove(Route.Param.COMPONENT_CLASS)
        params.remove(Route.Param.COMPONENT_TAG)
        params.remove(Route.Param.FRAGMENT_CONTAINER)
        return params.toMap()
    }

}
