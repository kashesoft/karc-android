/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.core.presenter

import com.kashesoft.karc.app.Application
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface Presentable {

    val application: Application<*>

    fun onError(error: Throwable) {}

    fun attachToPresenter(presenterClass: KClass<*>) {
        @Suppress("UNCHECKED_CAST")
        val presenter: Presenter = application.getPresenters().firstOrNull {
            it::class.isSubclassOf(presenterClass)
        } ?: return
        presenter.attachPresentable(this)
    }

    fun detachFromPresenter(presenterClass: KClass<*>) {
        @Suppress("UNCHECKED_CAST")
        val presenter: Presenter = application.getPresenters().firstOrNull {
            it::class.isSubclassOf(presenterClass)
        } ?: return
        presenter.detachPresentable(this)
    }

}

