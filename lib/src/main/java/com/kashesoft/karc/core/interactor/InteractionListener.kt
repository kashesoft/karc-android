/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface InteractionListener<R> {
    fun onInteractionStatus(interactionStatus: InteractionStatus<R>)
}