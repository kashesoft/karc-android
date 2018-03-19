/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.util.Log
import com.kashesoft.karc.core.interactor.Gateway
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.Logging

abstract class Controller : Logging, Gateway, Routable {

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

    override fun doSetUp(params: Map<String, Any>) {
        if (logging) log("onSetUp: params = $params")
        onSetUp(params)
    }

    override fun doEnterForeground() {
        if (logging) log("onEnterForeground")
        onEnterForeground()
    }

    override fun doBecomeActive() {
        if (logging) log("onBecomeActive")
        onBecomeActive()
    }

    override fun doBecomeInactive() {
        if (logging) log("onBecomeInactive")
        onBecomeInactive()
    }

    override fun doEnterBackground() {
        if (logging) log("onEnterBackground")
        onEnterBackground()
    }

    override fun doTearDown() {
        if (logging) log("onTearDown")
        onTearDown()
    }

    //endregion

}
