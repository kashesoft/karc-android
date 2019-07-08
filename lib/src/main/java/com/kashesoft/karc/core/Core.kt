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
        components.forEach { (component, spec) ->
            if (spec.appLifecycle) {
                component.setState(state)
            }
        }
    }

    internal val componentProviders: MutableMap<KClass<*>, Provider<Component>> = mutableMapOf()
    private val components: MutableMap<Component, Spec> = mutableMapOf()

    @Synchronized
    internal fun registerComponent(component: Component, params: Map<String, Any>, mode: Mode, appLifecycle: Boolean) {
        val spec = Spec(WeakReference(component), State.DOWN, params, appLifecycle, mode, component.toString())
        components[component] = spec
    }

    @Synchronized
    internal fun unregisterComponent(component: Component) {
        components.remove(component)
    }

    @Synchronized
    internal fun specForComponent(component: Component): Spec {
        return components[component]!!
    }

    fun setComponentProvider(componentProvider: Provider<Component>, componentClass: KClass<*>) {
        this.componentProviders[componentClass] = componentProvider
    }

    @Synchronized
    fun component(componentClass: KClass<*>): Component? {
        return components.keys.firstOrNull {
            componentClass.java.isAssignableFrom(it::class.java)
        }
    }

    @Synchronized
    fun setUpComponent(componentClass: KClass<*>, params: Map<String, Any>, mode: Mode, appLifecycle: Boolean) {
        if (components.any { componentClass.java.isAssignableFrom(it::class.java) }) return
        val componentProvider = componentProviders.toList().firstOrNull { it.first == componentClass }?.second
        val component = if (componentProvider != null) componentProvider.get() else componentClass.createInstance() as Component
        registerComponent(component, params, mode, appLifecycle)
        if (appLifecycle) {
            component.setState(state)
        }
    }

    @Synchronized
    fun tearDownComponent(componentClass: KClass<*>) {
        val component = component(componentClass) ?: return
        component.setState(State.DOWN)
    }

}
