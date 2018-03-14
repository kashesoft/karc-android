/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.content.Context
import android.util.Log
import com.kashesoft.karc.core.interactor.Gateway
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.Logging

abstract class Controller : Logging, Gateway, Presentable, Routable {

    override lateinit var application: Application<*>
        internal set

    protected val context: Context
        get() = application.applicationContext

    protected open val logging = false

    private fun log(message: String) {
        Log.v(name, ":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    open fun onSetUp(params: Map<String, Any>) {}

    open fun onEnterForeground() {}

    open fun onBecomeActive() {}

    open fun onBecomeInactive() {}

    open fun onEnterBackground() {}

    open fun onTearDown() {}

    internal fun doSetUp(params: Map<String, Any>) {
        if (logging) log("onSetUp: params = $params")
        onSetUp(params)
    }

    internal fun doEnterForeground() {
        if (logging) log("onEnterForeground")
        onEnterForeground()
    }

    internal fun doBecomeActive() {
        if (logging) log("onBecomeActive")
        onBecomeActive()
    }

    internal fun doBecomeInactive() {
        if (logging) log("onBecomeInactive")
        onBecomeInactive()
    }

    internal fun doEnterBackground() {
        if (logging) log("onEnterBackground")
        onEnterBackground()
    }

    internal fun doTearDown() {
        if (logging) log("onTearDown")
        onTearDown()
    }

    //endregion

}
