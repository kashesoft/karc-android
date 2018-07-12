/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.util.Log
import com.kashesoft.karc.core.interactor.Gateway
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.Logging
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

abstract class Controller : Logging, Gateway, Routable {

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60000L
    }

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

    private var executorService: ExecutorService = Executors.newFixedThreadPool(1)
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
        val futureTask = asyncSafely(
                onTry = {
                    synchronized(lock) {
                        onInitialize()
                    }
                    synchronized(this) {
                        if (state != State.INITIALIZING) {
                            return@synchronized
                        }
                        state = State.INITIALIZED
                        if (logging) log("onInitializationSuccess")
                        onInitializationSuccess()
                    }
                },
                onCatch = { error ->
                    synchronized(this) {
                        if (state != State.INITIALIZING) {
                            return@synchronized
                        }
                        state = State.DEINITIALIZED
                        if (logging) log("onInitializationFailure: error = $error")
                        onInitializationFailure(error)
                    }
                }
        )
        cancelFutureTaskAfterDelayIfNeeded(futureTask, DEFAULT_TIMEOUT_MILLIS)
    }

    @Synchronized
    protected fun deinitialize() {
        state = State.DEINITIALIZING
        val futureTask = asyncSafely(
                onTry = {
                    synchronized(lock) {
                        onDeinitialize()
                    }
                    synchronized(this) {
                        if (state != State.DEINITIALIZING) {
                            return@synchronized
                        }
                        state = State.DEINITIALIZED
                        if (logging) log("onDeinitializationSuccess")
                        onDeinitializationSuccess()
                    }
                },
                onCatch = { error ->
                    synchronized(this) {
                        if (state != State.DEINITIALIZING) {
                            return@synchronized
                        }
                        state = State.INITIALIZED
                        if (logging) log("onDeinitializationFailure: error = $error")
                        onDeinitializationFailure(error)
                    }
                },
                onFinally = {
                    shutdownExecutorServiceIfDead()
                }
        )
        cancelFutureTaskAfterDelayIfNeeded(futureTask, DEFAULT_TIMEOUT_MILLIS)
    }

    @Synchronized
    private fun shutdownExecutorServiceIfDead() {
        if (isDead) {
            executorService.shutdown()
        }
    }

    private fun asyncSafely(
            onTry: () -> Unit,
            onCatch: (Throwable) -> Unit,
            onFinally: (() -> Unit)? = null
    ): Future<*> {
        return executorService.submit {
            try {
                onTry()
            } catch (e: Throwable) {
                onCatch(e)
            } finally {
                onFinally?.invoke()
            }
        }
    }

    private fun cancelFutureTaskAfterDelayIfNeeded(futureTask: Future<*>, delayMillis: Long) {
        android.os.Handler().postDelayed({
            if (!futureTask.isDone) {
                futureTask.cancel(true)
            }
        }, delayMillis)
    }

    //endregion

}
