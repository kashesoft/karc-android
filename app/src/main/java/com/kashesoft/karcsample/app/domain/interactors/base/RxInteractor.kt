/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.interactors.base

import com.kashesoft.karc.core.interactor.Interactor
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

open class RxInteractor : Interactor() {

    fun <R> start(
            observableGenerator: () -> Observable<R>,
            onNext: ((output: R) -> Unit)? = null,
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null,
            observableScheduler: Scheduler = Schedulers.io(),
            observerScheduler: Scheduler = AndroidSchedulers.mainThread(),
            vararg tags: String
    ) {
        RxInteraction(
                this,
                observableGenerator,
                onNext,
                onComplete,
                onError,
                observableScheduler,
                observerScheduler,
                *tags
        ).start()
    }

}
