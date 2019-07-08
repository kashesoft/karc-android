/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import com.kashesoft.karc.core.Component
import com.kashesoft.karc.core.Core
import com.kashesoft.karc.core.interactor.Interaction
import com.kashesoft.karc.core.interactor.InteractionListener
import com.kashesoft.karc.core.interactor.InteractionStatus
import com.kashesoft.karc.core.interactor.Interactor
import com.kashesoft.karc.core.log
import kotlin.reflect.KClass

abstract class Presenter(
        private vararg val interactors: Interactor
) : Component, InteractionListener<Any> {

    override val logging = true
    override val loggingLifecycle = false

    //region <==========|Lifecycle|==========>

    override fun willSetUp(params: Map<String, Any>) {
        addInteractionListener(this)
        attachPresentablesForPresenter(this)
    }

    override fun didTearDown() {
        removeInteractionListener(this)
        detachAllPresentable()
    }

    //region <==========|Presentables|==========>

    private val presentables: MutableList<Presentable> = mutableListOf()

    fun getPresentables(): List<Presentable> = presentables.toList()

    inline fun <reified V : Presentable> presentable(presentableClass: KClass<V>): V? {
        return getPresentables().firstOrNull {
            presentableClass.java.isAssignableFrom(it::class.java)
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
        log("onPresentableAttached: $presentable")
        onPresentableAttached(presentable)
    }

    fun <V : Presentable> detachPresentable(presentable: V) {
        val detached = presentables.remove(presentable)
        if (!detached) return
        log("onPresentableDetached: $presentable")
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
        log("onInteractionStatus[$interactionStatus]")
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

    companion object {

        private val presentableForPresenterClassesMap: MutableMap<Presentable, MutableSet<KClass<*>>> = mutableMapOf()

        @Synchronized
        internal fun attachPresentableToPresenterWithClass(presentable: Presentable, presenterClass: KClass<*>) {
            if (presentableForPresenterClassesMap[presentable] == null) {
                presentableForPresenterClassesMap[presentable] = mutableSetOf()
            }
            presentableForPresenterClassesMap[presentable]?.add(presenterClass)
            val presenter: Presenter = Core.component(presenterClass) as? Presenter ?: return
            presenter.attachPresentable(presentable)
        }

        @Synchronized
        internal fun detachPresentableFromPresenterWithClass(presentable: Presentable, presenterClass: KClass<*>) {
            presentableForPresenterClassesMap[presentable]?.remove(presenterClass)
            if (presentableForPresenterClassesMap[presentable]?.isEmpty() == true) {
                presentableForPresenterClassesMap.remove(presentable)
            }
            val presenter: Presenter = Core.component(presenterClass) as? Presenter ?: return
            presenter.detachPresentable(presentable)
        }

        private fun attachPresentablesForPresenter(presenter: Presenter) {
            presentableForPresenterClassesMap
                    .filter { (_, presenterClasses) -> presenterClasses.any { it == presenter::class } }
                    .map { it.key }
                    .forEach { presenter.attachPresentable(it) }
        }

    }

}
