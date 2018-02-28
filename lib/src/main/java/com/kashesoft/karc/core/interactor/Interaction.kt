/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import java.lang.ref.WeakReference

class Interaction<R> internal constructor(
        interactor: Interactor,
        private val observableGenerator: () -> Observable<R>,
        private val onNext: ((output: R) -> Unit)?,
        private val onComplete: (() -> Unit)?,
        private val onError: ((error: Throwable) -> Unit)?,
        private val observableScheduler: Scheduler,
        private val observerScheduler: Scheduler,
        vararg val tags: String
) {

    private val interactor: WeakReference<Interactor> = WeakReference(interactor)
    private var subscription: Disposable? = null
    private val listeners: MutableList<InteractionListener<R>> = mutableListOf()

    @Synchronized
    fun start() {
        onInteractionResult(InteractionState.started(this))
    }

    @Synchronized
    fun dispose() {
        if (subscription?.isDisposed == false) {
            subscription?.dispose()
            onInteractionResult(InteractionState.disposed(this))
            onInteractionResult(InteractionState.stopped(this))
        }
    }

    @Synchronized
    fun isStarted(): Boolean = subscription != null

    @Synchronized
    fun isStopped(): Boolean = subscription == null

    @Synchronized
    internal fun addListener(listener: InteractionListener<R>) {
        listeners.add(listener)
    }

    @Synchronized
    internal fun removeListener(listener: InteractionListener<R>) {
        listeners.remove(listener)
    }

    @Synchronized
    private fun onInteractionResult(interactionState: InteractionState<R>) {
        val interactor = this.interactor.get() ?: return
        if (interactionState.status == InteractionState.Status.STARTED) {
            subscription = generateSubscription()
            interactor.attachInteraction(this)
        }
        for (listener in listeners) {
            listener.onInteractionResult(interactionState)
        }
        if (interactionState.status == InteractionState.Status.STOPPED) {
            interactor.detachInteraction(this)
            subscription = null
        }
    }

    private fun generateSubscription(): Disposable {
        return observableGenerator()
                .subscribeOn(observableScheduler)
                .observeOn(observerScheduler)
                .subscribeWith(
                        object : DisposableObserver<R>() {
                            override fun onNext(data: R) {
                                onInteractionResult(InteractionState.next(this@Interaction, data))
                                onNext?.invoke(data)
                            }
                            override fun onComplete() {
                                onInteractionResult(InteractionState.completed(this@Interaction))
                                onInteractionResult(InteractionState.stopped(this@Interaction))
                                onComplete?.invoke()
                            }
                            override fun onError(error: Throwable) {
                                error.printStackTrace()
                                onInteractionResult(InteractionState.error(this@Interaction, error))
                                onInteractionResult(InteractionState.stopped(this@Interaction))
                                onError?.invoke(error)
                            }
                        }
                )
    }

}
