/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.router

interface Routable {
    fun route(query: Query): Boolean { return false }
}
