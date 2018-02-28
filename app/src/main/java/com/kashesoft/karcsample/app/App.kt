/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app

import com.kashesoft.karc.app.Application
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karcsample.app.data.controllers.AbcController
import com.kashesoft.karcsample.app.data.controllers.XyzController
import com.kashesoft.karcsample.app.domain.presenters.AbcPresenter
import com.kashesoft.karcsample.app.domain.presenters.XyzPresenter
import com.kashesoft.karcsample.app.domain.routers.MainRouter
import dagger.android.AndroidInjector
import javax.inject.Inject
import javax.inject.Provider

class App : Application<MainRouter>(), Routable {

    companion object {
        lateinit var instance: App
    }

    lateinit var appComponent: AppComponent

    @Inject override lateinit var router: MainRouter
    @Inject lateinit var abcControllerProvider: Provider<AbcController>
    @Inject lateinit var xyzControllerProvider: Provider<XyzController>
    @Inject lateinit var abcPresenterProvider: Provider<AbcPresenter>
    @Inject lateinit var xyzPresenterProvider: Provider<XyzPresenter>

    override fun onCreate() {
        instance = this
        super.onCreate()
        setControllerProvider(abcControllerProvider)
        setControllerProvider(xyzControllerProvider)
        setPresenterProvider(abcPresenterProvider)
        setPresenterProvider(xyzPresenterProvider)
    }

    override fun applicationInjector(): AndroidInjector<App> {
        appComponent = DaggerAppComponent
                .builder()
                .application(this)
                .build()
        return appComponent
    }

}
