/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KCallable

fun <T : Any> T.profileMethodBegin(blockingMaxTime: Long = MethodProfiler.defaultBlockingMaxTime, recursionMaxDepth: Long = MethodProfiler.defaultRecursionMaxDepth, method: String = function(1), beginTrace: String = trace(1)) {
    MethodProfiler.profileMethodBegin(method, this, blockingMaxTime, recursionMaxDepth, beginTrace)
}

fun <T : Any> T.profileMethodEnd(method: String = function(1), endTrace: String = function(1)) {
    MethodProfiler.profileMethodEnd(method, this, endTrace)
}

sealed class ProfileMethodEvent

abstract class ProfileMethodContextEvent internal constructor(
        spec: MethodSpec,
        context: MethodContext
) : ProfileMethodEvent() {

    val beginTrace: String = context.beginTrace
    val beginTimestamp: Long = context.beginTimestamp
    val eventTimestamp: Long = System.currentTimeMillis()
    val methodId: String = spec.id
    val threadInfo: String = spec.thread.get()?.toString() ?: "N/A"
    val stackTrace: String = spec.thread.get()?.stackTrace?.let { traceTpString(it) } ?: "N/A"

    internal val detailedInfo: String
        get() {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            val beginTimestampString = format.format(Date(beginTimestamp))
            val eventTimestampString = format.format(Date(eventTimestamp))
            return "during profiling $beginTrace\n" +
                    "started at $beginTimestampString, stopped at $eventTimestampString\n" +
                    "method id: $methodId\n" +
                    "thread: $threadInfo\n" +
                    "stack trace: $stackTrace"
        }

}

class DeadlockEvent internal constructor(
        spec: MethodSpec,
        context: MethodContext
) : ProfileMethodContextEvent(
        spec,
        context
) {

    val blockingTime: Long = context.blockingTime

    override fun toString(): String {
        return "Deadlock with blocking time = $blockingTime\n$detailedInfo"
    }

}

class BadRecursionEvent internal constructor(
        spec: MethodSpec,
        context: MethodContext
) : ProfileMethodContextEvent(
        spec,
        context
) {

    val recursionDepth: Long = context.recursionDepth

    override fun toString(): String {
        return "Bad recursion with recursion depth = $recursionDepth\n$detailedInfo"
    }

}

class InconsistentMethodProfilerEvent internal constructor(
        private val callable: KCallable<*>,
        private val trace: String
) : ProfileMethodEvent() {

    override fun toString(): String {
        return "Inconsistent ${callable.name}\n$trace"
    }

}

internal class MethodSpec(
        val id: String,
        val thread: WeakReference<Thread>
) {

    companion object {
        fun newInstance(method: String, caller: Any): MethodSpec {
            val callerClass = caller.javaClass.kotlin.qualifiedName
            val id = method.replace(callerClass!!, "$callerClass@${Integer.toHexString(caller.hashCode())}")
            return MethodSpec(id, WeakReference(Thread.currentThread()))
        }
    }

    private val threadHashCode: Int = thread.get().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodSpec) return false

        if (id != other.id) return false
        if (thread.get() != other.thread.get()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + threadHashCode
        return result
    }
}

internal class MethodContext(
        val beginTrace: String,
        val beginTimestamp: Long,
        val blockingMaxTime: Long,
        val recursionMaxDepth: Long
) {
    var blockingTime: Long = 0
    var recursionDepth: Long = 0
}

internal object MethodProfiler {

    const val DEFAULT_SAMPLE_STEP_TIME = 50L // in millis
    const val DEFAULT_BLOCKING_MAX_TIME = 10000L // in millis
    const val DEFAULT_RECURSION_MAX_DEPTH = 1000L

    @Volatile
    private var activated = false

    @Volatile
    var sampleStepTime: Long = DEFAULT_SAMPLE_STEP_TIME

    @Volatile
    var defaultBlockingMaxTime: Long = DEFAULT_BLOCKING_MAX_TIME

    @Volatile
    var defaultRecursionMaxDepth: Long = DEFAULT_RECURSION_MAX_DEPTH

    var eventListener: ((ProfileMethodEvent) -> Unit)? = null

    private val contextMap: MutableMap<MethodSpec, MethodContext> = mutableMapOf()

    internal fun activate() {
        activated = true
        Thread {
            while (activated) {
                Thread.sleep(sampleStepTime)
                getContextMap().forEach { (spec, context) ->
                    onMonitor(spec, context)
                }
            }
        }.start()
    }

    internal fun deactivate() {
        activated = false
    }

    @Synchronized
    internal fun profileMethodBegin(method: String, caller: Any, blockingMaxTime: Long, recursionMaxDepth: Long, beginTrace: String) {
        val spec = MethodSpec.newInstance(method, caller)
        val context = getContext(spec)
        if (context != null) {
            context.recursionDepth += 1
        } else {
            addContext(
                    spec,
                    MethodContext(
                            beginTrace,
                            System.currentTimeMillis(),
                            blockingMaxTime,
                            recursionMaxDepth
                    )
            )
        }
    }

    @Synchronized
    internal fun profileMethodEnd(method: String, caller: Any, endTrace: String) {
        val spec = MethodSpec.newInstance(method, caller)
        val context = getContext(spec)
        if (context == null) {
            onEvent(InconsistentMethodProfilerEvent(::profileMethodEnd, endTrace))
        } else if (context.recursionDepth > 0L) {
            context.recursionDepth -= 1
        } else {
            removeContext(spec)
        }
    }

    private fun onMonitor(spec: MethodSpec, context: MethodContext) {
        val thread = spec.thread.get()
        if (thread == null) {
            removeContext(spec)
            return
        }
        when (thread.state) {
            Thread.State.BLOCKED -> {
                context.blockingTime += sampleStepTime
                if (context.blockingTime > context.blockingMaxTime) {
                    val deadlockEvent = DeadlockEvent(spec, context)
                    onEvent(deadlockEvent)
                    removeContext(spec)
                }
            }
            else -> {
                context.blockingTime = 0
            }
        }
        if (context.recursionDepth > context.recursionMaxDepth) {
            val badRecursionEvent = BadRecursionEvent(spec, context)
            onEvent(badRecursionEvent)
            removeContext(spec)
        }
    }

    private fun onEvent(event: ProfileMethodEvent) {
        eventListener?.invoke(event)
    }

    @Synchronized
    private fun getContext(spec: MethodSpec): MethodContext? {
        return contextMap[spec]
    }

    @Synchronized
    private fun getContextMap(): Map<MethodSpec, MethodContext> {
        return contextMap.toMap()
    }

    @Synchronized
    private fun addContext(spec: MethodSpec, context: MethodContext) {
        contextMap[spec] = context
    }

    @Synchronized
    private fun removeContext(spec: MethodSpec) {
        contextMap.remove(spec)
    }

}
