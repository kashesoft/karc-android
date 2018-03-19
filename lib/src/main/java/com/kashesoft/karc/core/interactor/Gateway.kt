/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface Gateway {

    fun doSetUp(params: Map<String, Any>)

    fun doEnterForeground()

    fun doBecomeActive()

    fun doBecomeInactive()

    fun doEnterBackground()

    fun doTearDown()

}