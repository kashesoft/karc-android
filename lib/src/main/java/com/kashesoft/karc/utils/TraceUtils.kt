/*
 * Copyright (c) 2018 Kashesoft
 */

package com.kashesoft.karc.utils

fun function(depth: Int): String {
    val stackTrace = Thread.currentThread().stackTrace
    val currentMethod = ::function.name
    val thisMethodIndex = stackTrace.indexOfFirst { it.methodName == currentMethod }
    val stackTraceElement = stackTrace[thisMethodIndex + 1 + depth] ?: return ""
    return "${stackTraceElement.className}.${stackTraceElement.methodName}"
}

fun functionSimplified(depth: Int): String {
    val stackTrace = Thread.currentThread().stackTrace
    val currentMethod = ::function.name
    val thisMethodIndex = stackTrace.indexOfFirst { it.methodName == currentMethod }
    val stackTraceElement = stackTrace[thisMethodIndex + 1 + depth] ?: return ""
    return stackTraceElement.methodName
}

fun trace(depth: Int): String {
    val stackTrace = Thread.currentThread().stackTrace
    val currentMethod = (::trace).name
    val thisMethodIndex = stackTrace.indexOfFirst { it.methodName == currentMethod }
    val stackTraceElement = stackTrace[thisMethodIndex + 1 + depth] ?: return ""
    return stackTraceElement.toString()
}

fun traceSimplified(depth: Int): String {
    val stackTrace = Thread.currentThread().stackTrace
    val currentMethod = (::trace).name
    val thisMethodIndex = stackTrace.indexOfFirst { it.methodName == currentMethod }
    val stackTraceElement = stackTrace[thisMethodIndex + 1 + depth] ?: return ""
    return stackTraceElementToSimplifiedString(stackTraceElement)
}

private fun stackTraceElementToSimplifiedString(stackTraceElement: StackTraceElement): String {
    // Android-changed: When ART cannot find a line number, the lineNumber field is set
    // to the dex_pc and the fileName field is set to null.
    val result = StringBuilder()
    result.append(simpleClassName(stackTraceElement.className)).append(".").append(stackTraceElement.methodName)
    if (stackTraceElement.isNativeMethod) {
        result.append("(Native Method)")
    } else if (stackTraceElement.fileName != null) {
        if (stackTraceElement.lineNumber >= 0) {
            result.append("(").append(stackTraceElement.fileName).append(":").append(stackTraceElement.lineNumber).append(")")
        } else {
            result.append("(").append(stackTraceElement.fileName).append(")")
        }
    } else {
        if (stackTraceElement.lineNumber >= 0) {
            // The line number is actually the dex pc.
            result.append("(Unknown Source:").append(stackTraceElement.lineNumber).append(")")
        } else {
            result.append("(Unknown Source)")
        }
    }
    return result.toString()
}

private fun simpleClassName(className: String): String {
    return className.split(".").last()
}

internal fun traceTpString(ste: Array<StackTraceElement>): String {
    val sb = StringBuilder()
    for (st in ste) {
        sb.append(st.toString() + System.lineSeparator())
    }
    return sb.toString()
}