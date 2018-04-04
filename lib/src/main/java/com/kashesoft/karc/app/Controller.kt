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

    private var isDead = false

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

    @Synchronized
    override fun doSetUp(params: Map<String, Any>) {
        if (isDead) return
        if (logging) log("onSetUp: params = $params")
        onSetUp(params)
    }

    @Synchronized
    override fun doEnterForeground() {
        if (isDead) return
        if (logging) log("onEnterForeground")
        onEnterForeground()
    }

    @Synchronized
    override fun doBecomeActive() {
        if (isDead) return
        if (logging) log("onBecomeActive")
        onBecomeActive()
    }

    @Synchronized
    override fun doBecomeInactive() {
        if (isDead) return
        if (logging) log("onBecomeInactive")
        onBecomeInactive()
    }

    @Synchronized
    override fun doEnterBackground() {
        if (isDead) return
        if (logging) log("onEnterBackground")
        onEnterBackground()
    }

    @Synchronized
    override fun doTearDown() {
        if (isDead) return
        if (logging) log("onTearDown")
        onTearDown()
        isDead = true
    }

    //endregion

}
