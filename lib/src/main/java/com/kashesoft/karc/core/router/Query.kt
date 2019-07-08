/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

data class Query(
        val path: String,
        val params: Map<String, Any> = mapOf()
)
