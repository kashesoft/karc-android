/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.os.Handler
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
        initialize()
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
        deinitialize()
        isDead = true
    }

    //endregion

    //region <==========|Initialization/deinitialization|==========>

    enum class State {
        INITIALIZING, INITIALIZED, DEINITIALIZING, DEINITIALIZED
    }

    @get:Synchronized @set:Synchronized
    var state: State = State.DEINITIALIZED
        private set

    private var handler: Handler? = null
    private val lock = Any()

    open fun onInitialize() {}

    open fun onInitializationSuccess() {}

    open fun onInitializationFailure(e: Throwable) {}

    open fun onDeinitialize() {}

    open fun onDeinitializationSuccess() {}

    open fun onDeinitializationFailure(e: Throwable) {}

    protected fun <R> sync(block: () -> R): R {
        synchronized(lock) {
            return block()
        }
    }

    @Synchronized
    protected fun initialize() {
        state = State.INITIALIZING
        handler = asyncSafely(
                onTry = {
                    synchronized(lock) {
                        onInitialize()
                    }
                    synchronized(this) {
                        if (state != State.INITIALIZING) {
                            return@synchronized
                        }
                        state = State.INITIALIZED
                        onInitializationSuccess()
                    }
                },
                onCatch = { error ->
                    synchronized(this) {
                        if (state != State.INITIALIZING) {
                            return@synchronized
                        }
                        state = State.DEINITIALIZED
                        onInitializationFailure(error)
                    }
                },
                onFinally = {
                    disposeHandler()
                }
        )
    }

    @Synchronized
    protected fun deinitialize() {
        state = State.DEINITIALIZING
        handler = asyncSafely(
                onTry = {
                    synchronized(lock) {
                        onDeinitialize()
                    }
                    synchronized(this) {
                        if (state != State.DEINITIALIZING) {
                            return@synchronized
                        }
                        state = State.DEINITIALIZED
                        onDeinitializationSuccess()
                    }
                },
                onCatch = { error ->
                    synchronized(this) {
                        if (state != State.DEINITIALIZING) {
                            return@synchronized
                        }
                        state = State.INITIALIZED
                        onDeinitializationFailure(error)
                    }
                },
                onFinally = {
                    disposeHandler()
                }
        )
    }

    @Synchronized
    private fun disposeHandler() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    private fun asyncSafely(
            onTry: () -> Unit,
            onCatch: (Throwable) -> Unit,
            onFinally: () -> Unit
    ): Handler {
        val handler = Handler()
        handler.post {
            try {
                onTry()
            } catch (e: Throwable) {
                onCatch(e)
            } finally {
                onFinally()
            }
        }
        return handler
    }

    //endregion

}
