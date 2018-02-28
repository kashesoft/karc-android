/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.data

import com.kashesoft.karcsample.app.data.controllers.AbcController
import com.kashesoft.karcsample.app.data.controllers.UserController
import com.kashesoft.karcsample.app.data.controllers.XyzController
import com.kashesoft.karcsample.app.domain.gateways.UserGateway
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    internal fun provideUserGateway(): UserGateway {
        return UserController()
    }

    @Provides
    @Singleton
    internal fun provideAbcController(): AbcController {
        return AbcController()
    }

    @Provides
    @Singleton
    internal fun provideXyzController(): XyzController {
        return XyzController()
    }

}
