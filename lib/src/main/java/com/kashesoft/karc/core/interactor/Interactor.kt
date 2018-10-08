/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.interactor

import com.kashesoft.karc.utils.Logging

abstract class Interactor : Logging {

    override val logging = true

    private val interactions: MutableList<Interaction<Any>> = mutableListOf()
    private val listeners: MutableList<InteractionListener<Any>> = mutableListOf()

    @Synchronized
    fun cancel(vararg tags: String) {
        if (tags.isEmpty()) {
            interactions.toList().forEach { it.cancel() }
        } else {
            interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.cancel() }
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
    fun addListener(listener: InteractionListener<Any>, vararg tags: String) {
        interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.addListener(listener) }
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: InteractionListener<Any>, vararg tags: String) {
        interactions.filter { it.tags.any { tags.contains(it) } }.forEach { it.removeListener(listener) }
        listeners.remove(listener)
    }

    @Synchronized
    internal fun attachInteraction(interaction: Interaction<Any>) {
        interactions.add(interaction)
        listeners.forEach { interaction.addListener(it) }
    }

    @Synchronized
    internal fun detachInteraction(interaction: Interaction<Any>) {
        listeners.forEach { interaction.removeListener(it) }
        interactions.remove(interaction)
    }

}
