/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.data.gateways

import com.kashesoft.karc.core.gateway.GatewayImpl
import com.kashesoft.karcsample.app.domain.entities.User
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
import io.reactivex.Observable

class XyzGatewayImpl : GatewayImpl(), XyzGateway {

    override val logging = true

    private val userNames = listOf("Andrew Kashaed", "Anastasia Evsigneeva", "John Doe",
            "Lee Loo", "Lana Do", "Mick Sia", "Jack Black", "Bob Green", "Nick Yellow", "Billy Red")

    override fun fetchUsers(userIds: List<Int>): Observable<User> {
        return Observable.create<User> { emitter ->
            for ((index, id) in userIds.withIndex()) {
                try {
                    Thread.sleep(1000)
                } catch (e: Throwable) {
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
                val progress = 100 * (index + 1) / (userIds.size)
                val user = User(id, userNames[index], progress)
                emitter.onNext(user)
            }
            try {
                Thread.sleep(1000)
            } catch (e: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
            emitter.onComplete()
        }
    }

}