/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app

import com.kashesoft.karc.app.Application
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.*
import com.kashesoft.karcsample.app.data.gateways.AbcGatewayImpl
import com.kashesoft.karcsample.app.data.gateways.XyzGatewayImpl
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway

class App : Application<AppRouter>(), Routable {

    override val router: AppRouter = AppRouter()

    override fun onCreate() {
        activateMethodProfiler()
        activateObjectProfiler()
        setGatewayProvider(Provider<AbcGateway> { AbcGatewayImpl() })
        setGatewayProvider(Provider<XyzGateway> { XyzGatewayImpl() })
        super.onCreate()
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
