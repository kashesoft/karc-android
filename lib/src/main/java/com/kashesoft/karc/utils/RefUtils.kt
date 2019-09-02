/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

import java.lang.ref.WeakReference

inline fun <T, R> T.withWeakThis(block: (WeakReference<T>) -> R): R {
    return block(WeakReference(this))
}
