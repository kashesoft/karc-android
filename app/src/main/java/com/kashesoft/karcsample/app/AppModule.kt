/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    internal fun provideContext(application: Application): Context {
        return application.applicationContext
    }

}
