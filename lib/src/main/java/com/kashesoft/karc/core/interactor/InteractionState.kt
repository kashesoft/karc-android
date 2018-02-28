/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

interface InteractionState<out R> {

    enum class Status { STARTED, NEXT, COMPLETED, ERROR, DISPOSED, STOPPED }

    val status: Status
    val interaction: Interaction<*>
    val data: R?
    val error: Throwable?

    val hasData: Boolean
        get() = data != null

    val hasError: Boolean
        get() = error != null

    companion object {

        fun <T> started(interaction: Interaction<T>): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.STARTED
                override val interaction = interaction
                override val data: T? = null
                override val error: Throwable? = null
            }
        }

        fun <T> next(interaction: Interaction<T>, data: T? = null): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.NEXT
                override val interaction = interaction
                override val data: T? = data
                override val error: Throwable? = null
            }
        }

        fun <T> completed(interaction: Interaction<T>): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.COMPLETED
                override val interaction = interaction
                override val data: T? = null
                override val error: Throwable? = null
            }
        }

        fun <T> error(interaction: Interaction<T>, error: Throwable): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.ERROR
                override val interaction = interaction
                override val data: T? = null
                override val error: Throwable? = error
            }
        }

        fun <T> disposed(interaction: Interaction<T>): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.DISPOSED
                override val interaction = interaction
                override val data: T? = null
                override val error: Throwable? = null
            }
        }

        fun <T> stopped(interaction: Interaction<T>): InteractionState<T> {
            return object : InteractionState<T> {
                override val status = Status.STOPPED
                override val interaction = interaction
                override val data: T? = null
                override val error: Throwable? = null
            }
        }

    }

}