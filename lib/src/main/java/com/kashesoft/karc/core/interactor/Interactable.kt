/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface Interactable {
    fun isCanceled(): Boolean
    fun cancel()
}
