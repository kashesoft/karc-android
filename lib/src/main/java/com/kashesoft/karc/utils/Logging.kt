/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

import android.util.Log

interface Logging {

    val name: String
        get() = this::class.simpleName!!

    val logging: Boolean

    fun logVerbose(message: String) {
        if (logging) Log.v(name, message)
    }

    fun logDebug(message: String) {
        if (logging) Log.d(name, message)
    }

    fun logInfo(message: String) {
        if (logging) Log.i(name, message)
    }

    fun logWarn(message: String) {
        if (logging) Log.w(name, message)
    }

    fun logError(message: String) {
        if (logging) Log.e(name, message)
    }

}
