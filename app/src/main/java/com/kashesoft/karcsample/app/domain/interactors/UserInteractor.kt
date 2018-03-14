/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.interactors

import com.kashesoft.karc.core.interactor.Interactor
import com.kashesoft.karcsample.app.domain.entities.User
import com.kashesoft.karcsample.app.domain.gateways.UserGateway

class UserInteractor : Interactor() {

    fun loadUsers(
            userIds: List<Int>,
            onNext: ((output: User) -> Unit)? = null,
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null
    ) {
        start(
                { gateway(UserGateway::class)!!.fetchUsers(userIds) },
                onNext = onNext,
                onComplete = onComplete,
                onError = onError
        )
    }

}