/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.presenters

import com.kashesoft.karcsample.app.domain.interactors.UserInteractor
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter

class MainPresenter(
        private val userInteractor: UserInteractor = UserInteractor()
) : UserPresenter(userInteractor) {

    override fun onEnterBackground() {
        //AppRouter::class.get.startActivity(
        //        DetailsActivity::class,
        //        mapOf("string" to "test string", "number" to 123)
        //).showFragmentInContainer(SecondFragment::class, R.id.container).route()
    }

}
