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
    private var presenterTag: String? = null

    private var isChangingConfigurations = false

    internal fun getPresenter(): P? {
        val presenterClass = presenterClass ?: return null
        val presenterTag = presenterTag ?: return null
        return Core.component(presenterClass, presenterTag) as? P
    }

    internal fun setPresenter(presenterClass: KClass<P>, presenterTag: String, params: Map<String, Any>) {
        this.presenterClass = presenterClass
        this.presenterTag = presenterTag
        Core.setUpComponent(presenterClass, presenterTag, params, Mode.UI_SYNC, false)
    }

    internal fun onCreate() {
        getPresenter()?.setState(State.BACKGROUND)
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
        val presenterClass = presenterClass
        val presenterTag = presenterTag
        if (presenterClass != null && presenterTag != null) {
            Core.tearDownComponent(presenterClass, presenterTag)
        }
        this.presenterClass = null
        this.presenterTag = null
    }

}
