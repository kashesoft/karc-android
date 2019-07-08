/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.util.concurrent.Semaphore

class CustomHandler(
        private val async: Boolean,
        looper: Looper
) : Handler(
        looper
) {

    companion object {
        private fun startHandlerThread(name: String, priority: Int): Looper {
            val semaphore = Semaphore(0)
            val handlerThread = object : HandlerThread(name, priority) {
                override fun onLooperPrepared() {
                    semaphore.release()
                }
            }
            handlerThread.start()
            semaphore.acquireUninterruptibly()
            return handlerThread.looper
        }
    }

    constructor(async: Boolean, threadName: String, threadPriority: Int) : this(async, startHandlerThread(threadName, threadPriority))

    constructor(async: Boolean, threadName: String) : this(async, threadName, Process.THREAD_PRIORITY_BACKGROUND)

    constructor(async: Boolean) : this(async, Looper.getMainLooper())

    fun quit() {
        looper.quit()
    }

    fun quitSafely() {
        looper.quitSafely()
    }

    fun join() {
        looper.thread.join()
    }

    fun execute(block: () -> Unit) {
        if (async || Looper.myLooper() != looper) {
            post(block)
        } else {
            block()
        }
    }

}
