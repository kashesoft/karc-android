/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core

import com.kashesoft.karc.utils.CustomHandler
import com.kashesoft.karc.utils.Logging
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

enum class State {
    DOWN, BACKGROUND, INACTIVE, ACTIVE
}

enum class Mode {
    UI_SYNC, UI_ASYNC, IO_SYNC, IO_ASYNC
}

class Spec(
        val component: WeakReference<Component>,
        var state: State,
        val params: Map<String, Any>,
        val appLifecycle: Boolean,
        mode: Mode,
        name: String
) {

    private val queue: CustomHandler = when (mode) {
        Mode.UI_SYNC -> CustomHandler(false)
        Mode.UI_ASYNC -> CustomHandler(true)
        Mode.IO_SYNC -> CustomHandler(false, "${name}HandlerThread")
        Mode.IO_ASYNC -> CustomHandler(true, "${name}HandlerThread")
    }

    private var transaction: Transaction? = null

    private var countDownLatch: CountDownLatch? = null

    fun dispatchTransaction(transaction: Transaction) {
        willStartTransaction()
        queue.removeCallbacksAndMessages(null)
        cancelTransactionIfExists()
        queue.execute {
            component.get()?.let {
                setTransaction(transaction)
                transaction.execute(it)
                removeTransaction()
            }
            didStopTransaction()
        }
    }

    fun <R> awaitAllTransactionsAndDo(block: () -> R): R {
        synchronized(this) { countDownLatch }?.let { it.await() }
        return block()
    }

    @Synchronized
    private fun cancelTransactionIfExists() {
        transaction?.canceled = true
    }

    @Synchronized
    private fun setTransaction(transaction: Transaction) {
        this.transaction = transaction
    }

    @Synchronized
    private fun removeTransaction() {
        this.transaction = null
    }

    private var currentTransactionCount: Int = 0

    @Synchronized
    private fun willStartTransaction() {
        if (currentTransactionCount == 0) {
            countDownLatch = CountDownLatch(1)
        }
        currentTransactionCount++
    }

    @Synchronized
    private fun didStopTransaction() {
        currentTransactionCount--
        if (currentTransactionCount == 0) {
            countDownLatch?.countDown()
            countDownLatch = null
        }
    }

}

class Transaction(
        private val block: (component: Component, transaction: Transaction) -> Unit
) {

    @get:Synchronized @set:Synchronized
    var canceled: Boolean = false

    fun execute(component: Component) {
        block(component, this)
    }

}

interface Component : Logging {

    val loggingLifecycle: Boolean

    fun willSetUp(params: Map<String, Any>) {}
    fun onSetUp(params: Map<String, Any>) {}
    fun didSetUp(params: Map<String, Any>) {}

    fun willEnterForeground() {}
    fun onEnterForeground() {}
    fun didEnterForeground() {}

    fun willBecomeActive() {}
    fun onBecomeActive() {}
    fun didBecomeActive() {}

    fun willBecomeInactive() {}
    fun onBecomeInactive() {}
    fun didBecomeInactive() {}

    fun willEnterBackground() {}
    fun onEnterBackground() {}
    fun didEnterBackground() {}

    fun willTearDown() {}
    fun onTearDown() {}
    fun didTearDown() {}

    var state: State
        get() = Core.specForComponent(this).state
        set(value) {
            Core.specForComponent(this).state = value
        }

    val params: Map<String, Any>
        get() = Core.specForComponent(this).params

}

fun <R> Component.sync(block: () -> R): R {
    return Core.specForComponent(this).awaitAllTransactionsAndDo { block() }
}

internal fun Component.setState(state: State) {
    val transaction = Transaction { component, transaction ->
        component.transaction(transaction, toState = state)
    }
    Core.specForComponent(this).dispatchTransaction(transaction)
}

@Synchronized
private fun Component.transaction(transaction: Transaction, toState: State) {
    val fromState = state
    if (fromState < toState) {
        if (fromState == State.DOWN) {
            doSetUp(params)
            state = State.BACKGROUND
            if (toState == State.BACKGROUND || transaction.canceled) return
        }
        if (fromState <= State.BACKGROUND) {
            doEnterForeground()
            state = State.INACTIVE
            if (toState == State.INACTIVE || transaction.canceled) return
        }
        if (fromState <= State.INACTIVE) {
            doBecomeActive()
            state = State.ACTIVE
        }
    } else if (fromState > toState) {
        if (fromState == State.ACTIVE) {
            doBecomeInactive()
            state = State.INACTIVE
            if (toState == State.INACTIVE || transaction.canceled) return
        }
        if (fromState >= State.INACTIVE) {
            doEnterBackground()
            state = State.BACKGROUND
            if (toState == State.BACKGROUND || transaction.canceled) return
        }
        if (fromState >= State.BACKGROUND) {
            doTearDown()
            state = State.DOWN
            Core.unregisterComponent(this)
        }
    }
}

private fun Component.doSetUp(params: Map<String, Any>) {
    willSetUp(params)
    log("onSetUp: params = $params")
    onSetUp(params)
    didSetUp(params)
}

private fun Component.doEnterForeground() {
    willEnterForeground()
    log("onEnterForeground")
    onEnterForeground()
    didEnterForeground()
}

private fun Component.doBecomeActive() {
    willBecomeActive()
    log("onBecomeActive")
    onBecomeActive()
    didBecomeActive()
}

private fun Component.doBecomeInactive() {
    willBecomeInactive()
    log("onBecomeInactive")
    onBecomeInactive()
    didBecomeInactive()
}

private fun Component.doEnterBackground() {
    willEnterBackground()
    log("onEnterBackground")
    onEnterBackground()
    didEnterBackground()
}

private fun Component.doTearDown() {
    willTearDown()
    log("onTearDown")
    onTearDown()
    didTearDown()
}

internal fun Component.log(message: String) {
    if (loggingLifecycle) logVerbose(":::::::::::::::$message:::::::::::::::")
}
