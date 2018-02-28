/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface InteractionListener<in R> {
    fun onInteractionResult(interactionState: InteractionState<R>)
}