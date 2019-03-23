/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.gateway

interface Gateway {

    val inForeground: Boolean
    
    val isActive: Boolean

    fun doSetUp(params: Map<String, Any>)

    fun doEnterForeground()

    fun doBecomeActive()

    fun doBecomeInactive()

    fun doEnterBackground()

    fun doTearDown()

}
