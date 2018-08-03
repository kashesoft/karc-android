/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import com.kashesoft.karc.app.Application
import kotlin.reflect.KClass

interface Presentable {

    val application: Application<*>

    fun onError(error: Throwable) {}

    fun attachToPresenter(presenterClass: KClass<*>) {
        application.attachPresentableToPresenterWithClass(this, presenterClass)
    }

    fun detachFromPresenter(presenterClass: KClass<*>) {
        application.detachPresentableFromPresenterWithClass(this, presenterClass)
    }

}
