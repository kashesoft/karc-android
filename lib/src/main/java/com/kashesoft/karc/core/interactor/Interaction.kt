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

    @Synchronized
    fun start() {
        onInteractionStatus(InteractionStatus.Start(this))
    }

    @Synchronized
    fun succeed(data: R) {
        onInteractionStatus(InteractionStatus.Success(this, data))
    }

    @Synchronized
    fun finish() {
        onInteractionStatus(InteractionStatus.Finish(this))
        onInteractionStatus(InteractionStatus.Stop(this))
    }

    @Synchronized
    fun fail(error: Throwable) {
        error.printStackTrace()
        onInteractionStatus(InteractionStatus.Failure(this, error))
        onInteractionStatus(InteractionStatus.Stop(this))
    }

    @Synchronized
    fun cancel() {
        if (interactable?.isCanceled() == false) {
            interactable?.cancel()
            onInteractionStatus(InteractionStatus.Cancel(this))
            onInteractionStatus(InteractionStatus.Stop(this))
        }
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

    @Synchronized
    protected fun onInteractionStatus(interactionStatus: InteractionStatus<R>) {
        val interactor = this.interactor.get() ?: return
        if (interactionStatus is InteractionStatus.Start) {
            try {
                interactable = generateInteractable()
            } catch (error: Throwable) {
                error.printStackTrace()
                publishInteractionStatus(InteractionStatus.Failure(this, error))
                return
            }
            interactor.attachInteraction(this as Interaction<Any>)
        }
        publishInteractionStatus(interactionStatus)
        if (interactionStatus is InteractionStatus.Stop) {
            interactor.detachInteraction(this as Interaction<Any>)
            interactable = null
        }
    }

    private fun publishInteractionStatus(interactionStatus: InteractionStatus<R>) {
        for (listener in listeners) {
            listener.onInteractionStatus(interactionStatus)
        }
    }

    protected abstract fun generateInteractable(): Interactable

}
