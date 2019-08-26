/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app

import com.kashesoft.karc.app.Application
import com.kashesoft.karc.app.setProvider
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.utils.*
import com.kashesoft.karcsample.app.data.gateways.AbcGatewayImpl
import com.kashesoft.karcsample.app.data.gateways.XyzGatewayImpl
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway

class App : Application<AppRouter>(), Routable {

    override val loggingLifecycle = true

    override val router: AppRouter = AppRouter()

    override fun onCreate() {
        activateMethodProfiler()
        activateObjectProfiler()
        AbcGateway::class.setProvider(Provider { AbcGatewayImpl() })
        XyzGateway::class.setProvider(Provider { XyzGatewayImpl() })
        super.onCreate()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
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
