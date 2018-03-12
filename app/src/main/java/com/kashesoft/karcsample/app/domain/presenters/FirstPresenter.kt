/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.presenters

import com.kashesoft.karcsample.app.domain.interactors.UserInteractor
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter
import com.kashesoft.karcsample.app.domain.routers.MainRouter
import javax.inject.Inject

class FirstPresenter @Inject
constructor(
        router: MainRouter,
        private val userInteractor: UserInteractor = UserInteractor()
) : UserPresenter(userInteractor)
