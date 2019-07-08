/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.interactors.base

import com.kashesoft.karc.core.interactor.Interactable
import com.kashesoft.karc.core.interactor.Interaction
import com.kashesoft.karc.core.interactor.Interactor
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.observers.DisposableObserver

class RxInteraction<R> constructor(
        interactor: Interactor,
        private val observableGenerator: () -> Observable<R>,
        private val onNext: ((output: R) -> Unit)?,
        private val onComplete: (() -> Unit)?,
        private val onError: ((error: Throwable) -> Unit)?,
        private val observableScheduler: Scheduler,
        private val observerScheduler: Scheduler,
        vararg tags: String
) : Interaction<R>(interactor, *tags) {

    override fun generateInteractable(): Interactable {
        return observableGenerator()
                .subscribeOn(observableScheduler)
                .observeOn(observerScheduler)
                .subscribeWith(
                        object : DisposableObserver<R>(), Interactable {
                            override fun onNext(data: R) {
                                succeed(data)
                                onNext?.invoke(data)
                            }
                            override fun onComplete() {
                                finish()
                                onComplete?.invoke()
                            }
                            override fun onError(error: Throwable) {
                                fail(error)
                                onError?.invoke(error)
                            }
                            override fun isCanceled(): Boolean {
                                return isDisposed
                            }
                            override fun cancel() {
                                dispose()
                            }
                        }
                )
    }

}
