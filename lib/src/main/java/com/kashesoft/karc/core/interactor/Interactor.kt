/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

import com.kashesoft.karc.utils.Logging
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

abstract class Interactor : Logging {

    companion object {

        private val gateways: MutableSet<Gateway> = mutableSetOf()

        @Synchronized
        fun <G : Gateway> getGateway(gatewayClass: KClass<G>): G? {
            @Suppress("UNCHECKED_CAST")
            return gateways.firstOrNull {
                it::class.isSubclassOf(gatewayClass)
            } as? G
        }

        @Synchronized
        fun registerGateway(gateway: Gateway) {
            gateways.add(gateway)
        }

        @Synchronized
        fun unregisterGateway(gateway: Gateway) {
            gateways.remove(gateway)
        }

    }

    private val interactions: MutableList<Interaction<*>> = mutableListOf()
    private val listeners: MutableList<InteractionListener<Any?>> = mutableListOf()

    inline fun <reified G : Gateway, R> start(
            crossinline observableGenerator: (G) -> Observable<R>,
            noinline onNext: ((output: R) -> Unit)? = null,
            noinline onComplete: (() -> Unit)? = null,
            noinline onError: ((error: Throwable) -> Unit)? = null,
            observableScheduler: Scheduler = Schedulers.io(),
            observerScheduler: Scheduler = AndroidSchedulers.mainThread(),
            vararg tags: String
    ) {
        Interaction(
                this,
                {
                    val gateway = getGateway(G::class) ?: throw IllegalStateException("Gateway ${G::class} is not registered!")
                    return@Interaction observableGenerator(gateway)
                },
                onNext,
                onComplete,
                onError,
                observableScheduler,
                observerScheduler,
                *tags
        ).start()
    }

    @Synchronized
    fun dispose(vararg tags: String) {
        if (tags.isEmpty()) {
            interactions.forEach { it.dispose() }
        } else {
            interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.dispose() }
        }
    }

    @Synchronized
    fun isLoading(vararg tags: String): Boolean {
        return if (tags.isEmpty()) {
            interactions.isNotEmpty()
        } else {
            interactions.any { it.tags.any { tags.contains(it) } }
        }
    }

    @Synchronized
    fun addListener(listener: InteractionListener<Any?>, vararg tags: String) {
        interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.addListener(listener) }
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: InteractionListener<Any?>, vararg tags: String) {
        interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.removeListener(listener) }
        listeners.remove(listener)
    }

    @Synchronized
    internal fun attachInteraction(interaction: Interaction<*>) {
        interactions.add(interaction)
        listeners.forEach { interaction.addListener(it) }
    }

    @Synchronized
    internal fun detachInteraction(interaction: Interaction<*>) {
        listeners.forEach { interaction.removeListener(it) }
        interactions.remove(interaction)
    }

}
