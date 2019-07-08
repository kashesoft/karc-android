/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.app

import androidx.lifecycle.ViewModel
import com.kashesoft.karc.core.Core
import com.kashesoft.karc.core.Mode
import com.kashesoft.karc.core.State
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.setState
import kotlin.reflect.KClass

internal class ViewModel<P : Presenter> : ViewModel() {

    private var presenterClass: KClass<P>? = null

    private var isChangingConfigurations = false

    internal fun hasNoPresenter(presenterClass: KClass<P>): Boolean {
        return Core.component(presenterClass) == null
    }

    internal fun getPresenter(): P? {
        return presenterClass?.let { Core.component(it) } as? P
    }

    internal fun setPresenter(presenterClass: KClass<P>, params: Map<String, Any>) {
        this.presenterClass = presenterClass
        Core.setUpComponent(presenterClass, params, Mode.UI_SYNC, false)
    }

    internal fun onStart() {
        if (!isChangingConfigurations) {
            getPresenter()?.setState(State.INACTIVE)
        } else {
            isChangingConfigurations = false
        }
    }

    internal fun onResume() {
        getPresenter()?.setState(State.ACTIVE)
    }

    internal fun onPause(isChangingConfigurations: Boolean) {
        this.isChangingConfigurations = isChangingConfigurations
        if (!isChangingConfigurations) {
            getPresenter()?.setState(State.INACTIVE)
        }
    }

    internal fun onStop(isChangingConfigurations: Boolean) {
        this.isChangingConfigurations = isChangingConfigurations
        if (!isChangingConfigurations) {
            getPresenter()?.setState(State.BACKGROUND)
        }
    }

    override fun onCleared() {
        super.onCleared()
        presenterClass?.let {
            Core.tearDownComponent(it)
            presenterClass = null
        }
    }

}
