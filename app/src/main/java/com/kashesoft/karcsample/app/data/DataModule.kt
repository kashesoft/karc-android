/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.data

import com.kashesoft.karcsample.app.data.controllers.AbcController
import com.kashesoft.karcsample.app.data.controllers.XyzController
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
import dagger.Module
import dagger.Provides

@Module
class DataModule {

    @Provides
    internal fun provideAbcGateway(): AbcGateway {
        return AbcController()
    }

    @Provides
    internal fun provideXyzGateway(): XyzGateway {
        return XyzController()
    }

}
