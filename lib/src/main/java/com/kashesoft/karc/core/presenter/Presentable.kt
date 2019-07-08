/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import kotlin.reflect.KClass

interface Presentable {

    fun onError(error: Throwable) {}

    fun attachToPresenter(presenterClass: KClass<*>) {
        Presenter.attachPresentableToPresenterWithClass(this, presenterClass)
    }

    fun detachFromPresenter(presenterClass: KClass<*>) {
        Presenter.detachPresentableFromPresenterWithClass(this, presenterClass)
    }

}
