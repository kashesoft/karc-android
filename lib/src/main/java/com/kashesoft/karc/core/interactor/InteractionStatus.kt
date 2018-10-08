/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

sealed class InteractionStatus<R>(val interaction: Interaction<R>) {
    class Start<R>(interaction: Interaction<R>) : InteractionStatus<R>(interaction)
    class Success<R>(interaction: Interaction<R>, val data: R) : InteractionStatus<R>(interaction)
    class Failure<R>(interaction: Interaction<R>, val error: Throwable) : InteractionStatus<R>(interaction)
    class Finish<R>(interaction: Interaction<R>) : InteractionStatus<R>(interaction)
    class Cancel<R>(interaction: Interaction<R>) : InteractionStatus<R>(interaction)
    class Stop<R>(interaction: Interaction<R>) : InteractionStatus<R>(interaction)
}
