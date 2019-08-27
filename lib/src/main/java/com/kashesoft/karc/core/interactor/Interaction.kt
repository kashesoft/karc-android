/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.interactor

import java.lang.ref.WeakReference

abstract class Interaction<R> constructor(
        interactor: Interactor,
        vararg val tags: String
) {

    private val interactor: WeakReference<Interactor> = WeakReference(interactor)
    private var interactable: Interactable? = null
    private val listeners: MutableList<InteractionListener<R>> = mutableListOf()

    fun start() {
        onInteractionStatus(InteractionStatus.Start(this))
    }

    fun succeed(data: R) {
        onInteractionStatus(InteractionStatus.Success(this, data))
    }

    fun finish() {
        onInteractionStatus(InteractionStatus.Finish(this))
        onInteractionStatus(InteractionStatus.Stop(this))
    }

    fun fail(error: Throwable) {
        error.printStackTrace()
        onInteractionStatus(InteractionStatus.Failure(this, error))
        onInteractionStatus(InteractionStatus.Stop(this))
    }

    fun cancel() {
        if (!cancelIfNeeded()) return
        onInteractionStatus(InteractionStatus.Cancel(this))
        onInteractionStatus(InteractionStatus.Stop(this))
    }

    @Synchronized
    fun isStarted(): Boolean = interactable != null

    @Synchronized
    fun isStopped(): Boolean = interactable == null

    @Synchronized
    internal fun addListener(listener: InteractionListener<R>) {
        listeners.add(listener)
    }

    @Synchronized
    internal fun removeListener(listener: InteractionListener<R>) {
        listeners.remove(listener)
    }

    private fun onInteractionStatus(interactionStatus: InteractionStatus<R>) {
        val interactor = synchronized(this) { this.interactor.get() ?: return }
        if (interactionStatus is InteractionStatus.Start) {
            synchronized(this) {
                try {
                    interactable = generateInteractable()
                } catch (error: Throwable) {
                    error.printStackTrace()
                    publishInteractionStatus(InteractionStatus.Failure(this, error))
                    return
                }
            }
            interactor.attachInteraction(this as Interaction<Any>)
        }
        synchronized(this) {
            publishInteractionStatus(interactionStatus)
        }
        if (interactionStatus is InteractionStatus.Stop) {
            interactor.detachInteraction(this as Interaction<Any>)
            synchronized(this) {
                interactable = null
            }
        }
    }

    private fun publishInteractionStatus(interactionStatus: InteractionStatus<R>) {
        for (listener in listeners) {
            listener.onInteractionStatus(interactionStatus)
        }
    }

    @Synchronized
    private fun cancelIfNeeded(): Boolean {
        return if (interactable?.isCanceled() == false) {
            interactable?.cancel()
            true
        } else {
            false
        }
    }

    protected abstract fun generateInteractable(): Interactable

}
