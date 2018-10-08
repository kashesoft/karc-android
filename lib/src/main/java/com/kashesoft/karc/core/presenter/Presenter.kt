/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import android.util.Log
import com.kashesoft.karc.core.interactor.Interaction
import com.kashesoft.karc.core.interactor.InteractionListener
import com.kashesoft.karc.core.interactor.InteractionStatus
import com.kashesoft.karc.core.interactor.Interactor
import com.kashesoft.karc.utils.Logging
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

abstract class Presenter(
        private vararg val interactors: Interactor
) : Logging, InteractionListener<Any> {

    protected open val logging = false

    private var isDead = false

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

    @Synchronized
    internal fun doSetUp(params: Map<String, Any>) {
        if (isDead) return
        if (logging) log("onSetUp: params = $params")
        addInteractionListener(this)
        onSetUp(params)
    }

    @Synchronized
    internal fun doEnterForeground() {
        if (isDead) return
        if (logging) log("onEnterForeground")
        onEnterForeground()
    }

    @Synchronized
    internal fun doBecomeActive() {
        if (isDead) return
        if (logging) log("onBecomeActive")
        onBecomeActive()
    }

    @Synchronized
    internal fun doBecomeInactive() {
        if (isDead) return
        if (logging) log("onBecomeInactive")
        onBecomeInactive()
    }

    @Synchronized
    internal fun doEnterBackground() {
        if (isDead) return
        if (logging) log("onEnterBackground")
        onEnterBackground()
    }

    @Synchronized
    internal fun doTearDown() {
        if (isDead) return
        if (logging) log("onTearDown")
        removeInteractionListener(this)
        onTearDown()
        detachAllPresentable()
        isDead = true
    }

    //endregion

    //region <==========|Presentables|==========>

    private val presentables: MutableList<Presentable> = mutableListOf()

    fun getPresentables(): List<Presentable> = presentables.toList()

    inline fun <reified V : Presentable> presentable(presentableClass: KClass<V>): V? {
        return getPresentables().firstOrNull {
            it::class.isSubclassOf(presentableClass)
        } as? V
    }

    inline fun <reified V : Presentable> present(onPresentable: (V) -> Unit) {
        getPresentables().forEach {
            val presentable: V = (it as? V) ?: return@forEach
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
        val detached = presentables.remove(presentable)
        if (!detached) return
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

    protected open fun onInteractionStart(interaction: Interaction<Any>) {}

    protected open fun onInteractionSuccess(interaction: Interaction<Any>, data: Any) {}

    protected open fun onInteractionFailure(interaction: Interaction<Any>, error: Throwable) {}

    protected open fun onInteractionFinish(interaction: Interaction<Any>) {}

    protected open fun onInteractionCancel(interaction: Interaction<Any>) {}

    protected open fun onInteractionStop(interaction: Interaction<Any>) {}

    @Synchronized
    private fun addInteractionListener(listener: InteractionListener<Any>) {
        interactors.forEach { it.addListener(listener) }
    }

    @Synchronized
    private fun removeInteractionListener(listener: InteractionListener<Any>) {
        interactors.forEach { it.removeListener(listener) }
    }

    @Synchronized
    fun disposeInteractions(vararg tags: String) {
        interactors.forEach { it.cancel(*tags) }
    }

    fun isLoading(vararg tags: String): Boolean {
        return interactors.any { it.isLoading(*tags) }
    }

    final override fun onInteractionStatus(interactionStatus: InteractionStatus<Any>) {
        if (logging) log("onInteractionStatus[$interactionStatus]")
        when (interactionStatus) {
            is InteractionStatus.Start -> {
                onInteractionStart(interactionStatus.interaction)
            }
            is InteractionStatus.Success -> {
                onInteractionSuccess(interactionStatus.interaction, interactionStatus.data)
            }
            is InteractionStatus.Failure -> {
                onInteractionFailure(interactionStatus.interaction, interactionStatus.error)
            }
            is InteractionStatus.Finish -> {
                onInteractionFinish(interactionStatus.interaction)
            }
            is InteractionStatus.Cancel -> {
                onInteractionCancel(interactionStatus.interaction)
            }
            is InteractionStatus.Stop -> {
                onInteractionStop(interactionStatus.interaction)
            }
        }
    }

    //endregion

}
