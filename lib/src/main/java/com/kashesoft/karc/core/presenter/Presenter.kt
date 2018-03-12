/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import android.util.Log
import com.kashesoft.karc.core.interactor.Interaction
import com.kashesoft.karc.core.interactor.InteractionListener
import com.kashesoft.karc.core.interactor.InteractionState
import com.kashesoft.karc.core.interactor.Interactor
import com.kashesoft.karc.utils.Logging
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

abstract class Presenter(
        private vararg val interactors: Interactor
) : Logging, InteractionListener<Any?> {

    protected open val logging = false

    private fun log(message: String) {
        Log.v(name, ":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    open fun onSetUp(params: Map<String, Any>) {}

    open fun onEnterForeground() {}

    open fun onBecomeActive() {}

    open fun onBecomeInactive() {}

    open fun onEnterBackground() {}

    open fun onTearDown() {}

    internal fun doSetUp(params: Map<String, Any>) {
        if (logging) log("onSetUp: params = $params")
        addInteractionListener(this)
        onSetUp(params)
    }

    internal fun doEnterForeground() {
        if (logging) log("onEnterForeground")
        onEnterForeground()
    }

    internal fun doBecomeActive() {
        if (logging) log("onBecomeActive")
        onBecomeActive()
    }

    internal fun doBecomeInactive() {
        if (logging) log("onBecomeInactive")
        onBecomeInactive()
    }

    internal fun doEnterBackground() {
        if (logging) log("onEnterBackground")
        onEnterBackground()
    }

    internal fun doTearDown() {
        if (logging) log("onTearDown")
        removeInteractionListener(this)
        onTearDown()
        detachAllPresentable()
    }

    //endregion

    //region <==========|Presentables|==========>

    private val presentables: MutableList<Presentable> = mutableListOf()

    fun <V : Presentable> presentable(presentableClass: KClass<V>): V? {
        @Suppress("UNCHECKED_CAST")
        return presentables.firstOrNull {
            it::class.isSubclassOf(presentableClass)
        } as? V
    }

    fun <V : Presentable> present(onPresentable: (V) -> Unit) {
        presentables.forEach {
            @Suppress("UNCHECKED_CAST")
            val presentable = (it as? V) ?: return@forEach
            onPresentable(presentable)
        }
    }

    fun hasPresentableAttached(): Boolean {
        return presentables.isNotEmpty()
    }

    fun <V : Presentable> hasPresentableAttached(presentable: V): Boolean {
        return presentables.contains(presentable)
    }

    fun <V : Presentable> hasPresentableAttached(presentableClass: KClass<V>): Boolean {
        return presentables.any { it::class == presentableClass }
    }

    fun <V : Presentable> attachPresentable(presentable: V) {
        presentables.add(presentable)
        if (logging) log("onPresentableAttached: $presentable")
        onPresentableAttached(presentable)
    }

    fun <V : Presentable> detachPresentable(presentable: V) {
        presentables.remove(presentable)
        if (logging) log("onPresentableDetached: $presentable")
        onPresentableDetached(presentable)
    }

    private fun detachAllPresentable() {
        presentables.toList().forEach { detachPresentable(it) }
    }

    open fun <V : Presentable> onPresentableAttached(presentable: V) {}

    open fun <V : Presentable> onPresentableDetached(presentable: V) {}

    //endregion

    //region <==========|Interactors|==========>

    protected open fun onInteractionStarted(interaction: Interaction<*>) {}

    protected open fun onInteractionNext(interaction: Interaction<*>, data: Any) {}

    protected open fun onInteractionCompleted(interaction: Interaction<*>) {}

    protected open fun onInteractionError(interaction: Interaction<*>, error: Throwable) {}

    protected open fun onInteractionDisposed(interaction: Interaction<*>) {}

    protected open fun onInteractionStopped(interaction: Interaction<*>) {}

    @Synchronized
    private fun addInteractionListener(listener: InteractionListener<Any?>) {
        interactors.forEach { it.addListener(listener) }
    }

    @Synchronized
    private fun removeInteractionListener(listener: InteractionListener<Any?>) {
        interactors.forEach { it.removeListener(listener) }
    }

    @Synchronized
    fun disposeInteractions(vararg tags: String) {
        interactors.forEach { it.dispose(*tags) }
    }

    override fun onInteractionResult(interactionState: InteractionState<Any?>) {
        when (interactionState.status) {
            InteractionState.Status.STARTED -> {
                doInteractionStarted(interactionState.interaction)
            }
            InteractionState.Status.NEXT -> {
                doInteractionNext(interactionState.interaction, interactionState.data!!)
            }
            InteractionState.Status.COMPLETED -> {
                doInteractionCompleted(interactionState.interaction)
            }
            InteractionState.Status.ERROR -> {
                doInteractionError(interactionState.interaction, interactionState.error!!)
            }
            InteractionState.Status.DISPOSED -> {
                doInteractionDisposed(interactionState.interaction)
            }
            InteractionState.Status.STOPPED -> {
                doInteractionStopped(interactionState.interaction)
            }
        }
    }

    private fun doInteractionStarted(interaction: Interaction<*>) {
        if (logging) log("onInteractionStarted[${interaction::class.simpleName}]")
        onInteractionStarted(interaction)
    }

    private fun doInteractionNext(interaction: Interaction<*>, data: Any) {
        if (logging) log("onInteractionNext[${interaction::class.simpleName}, data=$data]")
        onInteractionNext(interaction, data)
    }

    private fun doInteractionCompleted(interaction: Interaction<*>) {
        if (logging) log("onInteractionCompleted[${interaction::class.simpleName}]")
        onInteractionCompleted(interaction)
    }

    private fun doInteractionError(interaction: Interaction<*>, error: Throwable) {
        if (logging) log("onInteractionError[${interaction::class.simpleName}, error=$error]")
        onInteractionError(interaction, error)
    }

    private fun doInteractionDisposed(interaction: Interaction<*>) {
        if (logging) log("onInteractionDisposed[${interaction::class.simpleName}]")
        onInteractionDisposed(interaction)
    }

    private fun doInteractionStopped(interaction: Interaction<*>) {
        if (logging) log("onInteractionStopped[${interaction::class.simpleName}]")
        onInteractionStopped(interaction)
    }

    //endregion

}
