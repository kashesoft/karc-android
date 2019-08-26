/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core

import com.kashesoft.karc.utils.Provider
import java.lang.ref.WeakReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Core {

    private var state: State = State.DOWN

    fun setState(state: State) {
        this.state = state
        copiedComponents().forEach { (component, spec) ->
            if (spec.appLifecycle) {
                component.setState(state)
            }
        }
    }

    internal val componentProviders: MutableMap<KClass<*>, Provider<Component>> = mutableMapOf()
    private val components: MutableMap<Component, Spec> = mutableMapOf()

    @Synchronized
    private fun copiedComponents(): List<Pair<Component, Spec>> {
        return components.toList()
    }

    @Synchronized
    internal fun registerComponent(component: Component, componentTag: String, params: Map<String, Any>, mode: Mode, appLifecycle: Boolean) {
        val spec = Spec(WeakReference(component), componentTag, State.DOWN, params, appLifecycle, mode, component.toString())
        components[component] = spec
    }

    @Synchronized
    internal fun unregisterComponent(component: Component) {
        components.remove(component)
    }

    @Synchronized
    internal fun specForComponent(component: Component): Spec? {
        return components[component]
    }

    fun setComponentProvider(componentProvider: Provider<Component>, componentClass: KClass<*>) {
        this.componentProviders[componentClass] = componentProvider
    }

    fun component(componentClass: KClass<*>, componentTag: String = "default"): Component? {
        return copiedComponents().firstOrNull { (component, spec) ->
            componentClass.java.isAssignableFrom(component::class.java) && spec.tag == componentTag
        }?.first
    }

    @Synchronized
    fun setUpComponent(componentClass: KClass<*>, componentTag: String, params: Map<String, Any>, mode: Mode, appLifecycle: Boolean) {
        if (components.any { componentClass.java.isAssignableFrom(it::class.java) }) return
        val componentProvider = componentProviders.toList().firstOrNull { it.first == componentClass }?.second
        val component = if (componentProvider != null) componentProvider.get() else componentClass.createInstance() as Component
        registerComponent(component, componentTag, params, mode, appLifecycle)
        if (appLifecycle) {
            component.setState(state)
        }
    }

    @Synchronized
    fun tearDownComponent(componentClass: KClass<*>, componentTag: String) {
        val component = component(componentClass, componentTag) ?: return
        component.setState(State.DOWN)
    }

}
