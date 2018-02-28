/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.utils

import android.util.Log

interface Logging {

    val name: String
        get() = this::class.simpleName!!

    fun logVerbose(message: String) {
        Log.v(name, message)
    }

    fun logDebug(message: String) {
        Log.d(name, message)
    }

    fun logInfo(message: String) {
        Log.i(name, message)
    }

    fun logWarn(message: String) {
        Log.w(name, message)
    }

    fun logError(message: String) {
        Log.e(name, message)
    }

}
