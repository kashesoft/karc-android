/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain

import com.kashesoft.karcsample.app.domain.gateways.UserGateway
import com.kashesoft.karcsample.app.domain.interactors.UserInteractor
import com.kashesoft.karcsample.app.domain.presenters.*
import com.kashesoft.karcsample.app.domain.routers.MainRouter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DomainModule {

    @Provides
    @Singleton
    internal fun provideMainRouter(): MainRouter {
        return MainRouter()
    }

    @Provides
    internal fun provideMainPresenter(
            mainRouter: MainRouter,
            userInteractor: UserInteractor
    ): MainPresenter {
        return MainPresenter(mainRouter, userInteractor)
    }

    @Provides
    internal fun provideFirstPresenter(
            mainRouter: MainRouter,
            userInteractor: UserInteractor
    ): FirstPresenter {
        return FirstPresenter(mainRouter, userInteractor)
    }

    @Provides
    internal fun provideSecondPresenter(
            mainRouter: MainRouter,
            userInteractor: UserInteractor
    ): SecondPresenter {
        return SecondPresenter(mainRouter, userInteractor)
    }

    @Provides
    internal fun provideAbcPresenter(
    ): AbcPresenter {
        return AbcPresenter()
    }

    @Provides
    internal fun provideXyzPresenter(
    ): XyzPresenter {
        return XyzPresenter()
    }

    @Provides
    @Singleton
    internal fun provideUserInteractor(userGateway: UserGateway): UserInteractor {
        return UserInteractor(userGateway)
    }

}
