/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface InteractionListener<R> {
    fun onInteractionStatus(interactionStatus: InteractionStatus<R>)
}