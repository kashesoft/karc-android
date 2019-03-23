/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.interactors

import com.kashesoft.karc.app.get
import com.kashesoft.karcsample.app.domain.entities.User
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.interactors.base.RxInteractor

class UserInteractor : RxInteractor() {

    fun loadUsers(
            userIds: List<Int>,
            onNext: ((output: User) -> Unit)? = null,
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null
    ) {
        start(
                { AbcGateway::class.get.fetchUsers(userIds) },
                onNext = onNext,
                onComplete = onComplete,
                onError = onError
        )
    }
    fun loadUsers2(
            userIds: List<Int>,
            onNext: ((output: User) -> Unit)? = null,
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null
    ) {
        start(
                { AbcGateway::class.get.fetchUsers2(userIds) },
                onNext = onNext,
                onComplete = onComplete,
                onError = onError
        )
    }

    fun badRecursion(
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null
    ) {
        start(
                { AbcGateway::class.get.badRecursion().toObservable<Nothing>() },
                onComplete = onComplete,
                onError = onError
        )
    }

}