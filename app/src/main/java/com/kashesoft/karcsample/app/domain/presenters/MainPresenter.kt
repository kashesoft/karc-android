/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.presenters

import com.kashesoft.karcsample.app.domain.interactors.UserInteractor
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter

class MainPresenter(
        private val userInteractor: UserInteractor = UserInteractor()
) : UserPresenter(userInteractor)