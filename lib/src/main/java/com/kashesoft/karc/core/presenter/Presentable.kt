/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import com.kashesoft.karc.app.Application
import kotlin.reflect.KClass

interface Presentable {

    fun onError(error: Throwable) {}

    fun attachToPresenter(presenterClass: KClass<*>) {
        Application.instance.attachPresentableToPresenterWithClass(this, presenterClass)
    }

    fun detachFromPresenter(presenterClass: KClass<*>) {
        Application.instance.detachPresentableFromPresenterWithClass(this, presenterClass)
    }

}
