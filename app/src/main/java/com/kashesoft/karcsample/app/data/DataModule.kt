/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.data

import com.kashesoft.karcsample.app.data.gateways.AbcGatewayImpl
import com.kashesoft.karcsample.app.data.gateways.XyzGatewayImpl
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
import dagger.Module
import dagger.Provides

@Module
class DataModule {

    @Provides
    internal fun provideAbcGateway(): AbcGateway {
        return AbcGatewayImpl()
    }

    @Provides
    internal fun provideXyzGateway(): XyzGateway {
        return XyzGatewayImpl()
    }

}
