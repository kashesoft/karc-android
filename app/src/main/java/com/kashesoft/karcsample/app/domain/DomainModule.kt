/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain

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
            mainRouter: MainRouter
    ): MainPresenter {
        return MainPresenter(mainRouter)
    }

    @Provides
    internal fun provideFirstPresenter(
            mainRouter: MainRouter
    ): FirstPresenter {
        return FirstPresenter(mainRouter)
    }

    @Provides
    internal fun provideSecondPresenter(
            mainRouter: MainRouter
    ): SecondPresenter {
        return SecondPresenter(mainRouter)
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

}
