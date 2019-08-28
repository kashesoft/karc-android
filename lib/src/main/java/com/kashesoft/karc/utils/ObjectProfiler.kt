/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KCallable

fun <T : Any> T.profileObjectDidCreate(createTrace: String = trace(1)) {
    ObjectProfiler.profileObjectDidCreate(this, createTrace)
}

fun <T : Any> T.profileObjectWillDestroy(deleteTrace: String = trace(1)) {
    ObjectProfiler.profileObjectWillDestroy(this, deleteTrace)
}

internal data class ObjectSpec(val id: String) {
    companion object {
        fun newInstance(obj: Any): ObjectSpec {
            return ObjectSpec("${obj.javaClass.kotlin.simpleName}@${Integer.toHexString(obj.hashCode())}")
        }
    }
}

internal class ObjectContext(
        val createTrace: String,
        val createTimestamp: Long
) {
    var alive: Boolean = true
    var deleteTrace: String? = null
    var deleteTimestamp: Long? = null
}

sealed class ProfileObjectEvent

abstract class ProfileObjectContextEvent internal constructor(
        spec: ObjectSpec,
        context: ObjectContext
) : ProfileObjectEvent() {

    val id: String = spec.id
    val createTrace: String = context.createTrace
    val beginTimestamp: Long = context.createTimestamp
    val eventTimestamp: Long = System.currentTimeMillis()

    internal val detailedInfo: String
        get() {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            val beginTimestampString = format.format(Date(beginTimestamp))
            val eventTimestampString = format.format(Date(eventTimestamp))
            return "during profiling $createTrace\n" +
                    "started at $beginTimestampString, stopped at $eventTimestampString\n" +
                    "object id: $id\n"
        }

}

class MemoryLeakEvent internal constructor(
        spec: ObjectSpec,
        context: ObjectContext
) : ProfileObjectContextEvent(
        spec,
        context
) {

    val deleteTrace: String = context.deleteTrace!!

    override fun toString(): String {
        return "Memory leak with delete trace = $deleteTrace\n$detailedInfo"
    }

}

class DidCreateEvent internal constructor(
        spec: ObjectSpec,
        context: ObjectContext
) : ProfileObjectContextEvent(
        spec,
        context
) {

    override fun toString(): String {
        return "Did create $id"
    }

}

class WillDestroyEvent internal constructor(
        spec: ObjectSpec,
        context: ObjectContext
) : ProfileObjectContextEvent(
        spec,
        context
) {

    override fun toString(): String {
        return "Will destroy $id"
    }

}

class DidDestroyEvent internal constructor(
        spec: ObjectSpec,
        context: ObjectContext
) : ProfileObjectContextEvent(
        spec,
        context
) {

    override fun toString(): String {
        return "Did destroy $id"
    }

}

class GcEvent internal constructor(
        private val backoffCounter: Int
) : ProfileObjectEvent() {

    override fun toString(): String {
        return "Runs the garbage collector, backoffCounter = $backoffCounter"
    }

}

class InconsistentObjectProfilerEvent internal constructor(
        private val callable: KCallable<*>,
        private val trace: String
) : ProfileObjectEvent() {

    override fun toString(): String {
        return "Inconsistent ${callable.name}\n$trace"
    }

}

internal object ObjectProfiler {

    const val DEFAULT_SAMPLE_STEP_TIME = 50L // in millis

    @Volatile
    private var activated = false

    @Volatile
    var sampleStepTime: Long = DEFAULT_SAMPLE_STEP_TIME

    private val FIBONACCI = intArrayOf(1, 2, 3, 5, 8, 13, 21)
    @Volatile
    private var backoffCounter = 0

    private val beforeGcQueue = ReferenceQueue<Any>()
    private val liveObjectReferences = HashMap<Any, ObjectSpec>()
    private val contextMap: MutableMap<ObjectSpec, ObjectContext> = mutableMapOf()

    var eventListener: ((ProfileObjectEvent) -> Unit)? = null

    @Volatile
    private var needGc = false

    internal fun activate() {
        activated = true
        Thread {
            while (activated) {
                Thread.sleep(50)
                beforeGcQueue.poll()?.let { ref ->
                    liveObjectReferences.remove(ref)?.let { spec ->
                        removeContext(spec)?.let { context ->
                            onEvent(DidDestroyEvent(spec, context))
                        }
                    }
                }
                if (needGc) {
                    needGc = false
                    backoffCounter = 1
                }
                if (backoffCounter == 0) continue
                if (backoffCounter == FIBONACCI.last() + 1) {
                    clearDestroyingSpecsNotifyingMemoryLeakIfNeeded()
                }
                if (noDestroyingSpecs()) {
                    backoffCounter = 0
                } else {
                    if (FIBONACCI.contains(backoffCounter)) {
                        onEvent(GcEvent(backoffCounter))
                        Runtime.getRuntime().gc()
                    }
                    backoffCounter++
                }
            }
        }.start()
    }

    internal fun deactivate() {
        activated = false
    }

    internal fun profileObjectDidCreate(obj: Any, createTrace: String) {
        val ref: Reference<*> = PhantomReference(obj, beforeGcQueue)
        val spec = ObjectSpec.newInstance(obj)
        liveObjectReferences[ref] = spec
        val context = ObjectContext(createTrace, System.currentTimeMillis())
        onEvent(DidCreateEvent(spec, context))
        addContext(spec, context)
    }

    internal fun profileObjectWillDestroy(obj: Any, deleteTrace: String) {
        val spec = ObjectSpec.newInstance(obj)
        val context = getContext(spec)
        if (context == null) {
            ObjectProfiler.onEvent(InconsistentObjectProfilerEvent(::profileObjectWillDestroy, deleteTrace))
        } else {
            context.alive = false
            context.deleteTrace = deleteTrace
            context.deleteTimestamp = System.currentTimeMillis()
            onEvent(WillDestroyEvent(spec, context))
        }
        needGc = true
    }

    private fun onEvent(event: ProfileObjectEvent) {
        ObjectProfiler.eventListener?.invoke(event)
    }

    @Synchronized
    private fun noDestroyingSpecs(): Boolean {
        return contextMap.none { !it.value.alive }
    }

    @Synchronized
    private fun clearDestroyingSpecsNotifyingMemoryLeakIfNeeded() {
        contextMap.filter { !it.value.alive }.forEach { (spec, context) ->
            onMemoryLeak(spec, context)
            removeContext(spec)
        }
    }

    private fun onMemoryLeak(spec: ObjectSpec, context: ObjectContext) {
        val memoryLeakEvent = MemoryLeakEvent(spec, context)
        onEvent(memoryLeakEvent)
    }

    @Synchronized
    private fun getContext(spec: ObjectSpec): ObjectContext? {
        return contextMap[spec]
    }

    @Synchronized
    private fun getContextMap(): Map<ObjectSpec, ObjectContext> {
        return contextMap.toMap()
    }

    @Synchronized
    private fun addContext(spec: ObjectSpec, context: ObjectContext) {
        contextMap[spec] = context
    }

    @Synchronized
    private fun removeContext(spec: ObjectSpec): ObjectContext? {
        return contextMap.remove(spec)
    }

}
