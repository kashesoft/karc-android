/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import androidx.lifecycle.ViewModel
import com.kashesoft.karc.core.presenter.Presenter

class ViewModel<P : Presenter> : ViewModel() {

    private var presenter: P? = null

    internal fun getPresenter(): P? {
        return this.presenter
    }

    internal fun setPresenter(presenter: P, params: Map<String, Any>) {
        this.presenter = presenter
        presenter.doSetUp(params)
    }

    override fun onCleared() {
        super.onCleared()
        presenter?.doTearDown()
        presenter = null
    }

}
