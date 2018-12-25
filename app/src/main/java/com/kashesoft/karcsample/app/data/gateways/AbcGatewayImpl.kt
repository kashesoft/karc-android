/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.data.gateways

import com.kashesoft.karc.core.gateway.GatewayImpl
import com.kashesoft.karc.utils.profileMethodBegin
import com.kashesoft.karc.utils.profileMethodEnd
import com.kashesoft.karcsample.app.domain.entities.User
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import io.reactivex.Completable
import io.reactivex.Observable

class AbcGatewayImpl : GatewayImpl(), AbcGateway {

    override val logging = true

    companion object {
        val lock1: Any = Any()
        val lock2: Any = Any()
    }

    private val userNames = listOf("Andrew Black", "Anastasia Green", "John Doe",
            "Lee Loo", "Lana Do", "Mick Sia", "Jack Black", "Bob Green", "Nick Yellow", "Billy Red")

    override fun fetchUsers(userIds: List<Int>): Observable<User> {
        return Observable.create<User> { emitter ->
            profileMethodBegin(blockingMaxTime = 1000L)
            for ((index, id) in userIds.withIndex()) {
                try {
                    synchronized(lock1) {
                        Thread.sleep(1000)
                        synchronized(lock2) {

                        }
                    }
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

    override fun fetchUsers2(userIds: List<Int>): Observable<User> {
        return Observable.create<User> { emitter ->
            profileMethodBegin(blockingMaxTime = 2000L)
            for ((index, id) in userIds.withIndex()) {
                try {
                    synchronized(lock2) {
                        Thread.sleep(1000)
                        synchronized(lock1) {

                        }
                    }
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

    override fun badRecursion(): Completable {
        return Completable.fromAction {
            badRecursionCall()
        }
    }

    private fun badRecursionCall() {
        profileMethodBegin()
        Thread.sleep(2)
        badRecursionCall()
        profileMethodEnd()
    }

}