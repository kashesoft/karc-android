/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface Interactable {
    fun isCanceled(): Boolean
    fun cancel()
}
