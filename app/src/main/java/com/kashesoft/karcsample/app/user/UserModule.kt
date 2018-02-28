/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.user

import com.kashesoft.karcsample.app.user.activities.MainActivity
import com.kashesoft.karcsample.app.user.activities.DetailsActivity
import com.kashesoft.karcsample.app.user.fragments.FirstFragment
import com.kashesoft.karcsample.app.user.fragments.SecondFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class UserModule {

    @ContributesAndroidInjector
    internal abstract fun bindPrimaryActivity(): MainActivity

    @ContributesAndroidInjector
    internal abstract fun bindSecondaryActivity(): DetailsActivity

    @ContributesAndroidInjector
    internal abstract fun bindAbcFragment(): FirstFragment

    @ContributesAndroidInjector
    internal abstract fun bindXyzFragment(): SecondFragment

}
