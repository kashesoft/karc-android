/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app

import android.app.Application
import com.kashesoft.karcsample.app.data.DataModule
import com.kashesoft.karcsample.app.domain.DomainModule
import com.kashesoft.karcsample.app.user.UserModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    AppModule::class,
    UserModule::class,
    DomainModule::class,
    DataModule::class
])
interface AppComponent : AndroidInjector<App> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): AppComponent
    }

}
