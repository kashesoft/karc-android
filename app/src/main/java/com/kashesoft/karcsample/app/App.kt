/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app

import com.kashesoft.karc.app.Application
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.*
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
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
    @Inject lateinit var abcGatewayProvider: Provider<AbcGateway>
    @Inject lateinit var xyzGatewayProvider: Provider<XyzGateway>
    @Inject lateinit var abcPresenterProvider: Provider<AbcPresenter>
    @Inject lateinit var xyzPresenterProvider: Provider<XyzPresenter>

    override fun onCreate() {
        instance = this
        activateMethodProfiler()
        activateObjectProfiler()
        super.onCreate()
        setGatewayProvider(abcGatewayProvider)
        setGatewayProvider(xyzGatewayProvider)
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

    override fun onMethodProfilerEvent(event: ProfileMethodEvent) {
        when (event) {
            is DeadlockEvent, is BadRecursionEvent -> {
                logError(event.toString())
            }
            is InconsistentMethodProfilerEvent -> {
                logWarn(event.toString())
            }
        }
    }

    override fun onObjectProfilerEvent(event: ProfileObjectEvent) {
        when (event) {
            is MemoryLeakEvent -> {
                logError(event.toString())
            }
            is InconsistentObjectProfilerEvent -> {
                logWarn(event.toString())
            }
            is DidCreateEvent, is WillDestroyEvent, is DidDestroyEvent -> {
                logInfo(event.toString())
            }
            is GcEvent -> {
                logDebug(event.toString())
            }
        }
    }

}
