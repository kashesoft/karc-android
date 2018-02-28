/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

import com.kashesoft.karc.utils.Logging
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

abstract class Interactor : Logging {

    private val interactions: MutableList<Interaction<*>> = mutableListOf()
    private val listeners: MutableList<InteractionListener<Any?>> = mutableListOf()

    fun <R> start(
            observableGenerator: () -> Observable<R>,
            onNext: ((output: R) -> Unit)? = null,
            onComplete: (() -> Unit)? = null,
            onError: ((error: Throwable) -> Unit)? = null,
            observableScheduler: Scheduler = Schedulers.io(),
            observerScheduler: Scheduler = AndroidSchedulers.mainThread(),
            vararg tags: String
    ) {
        Interaction(
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
