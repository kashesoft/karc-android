/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.presenters

import com.kashesoft.karcsample.app.domain.interactors.UserInteractor
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter

class FirstPresenter (
        private val userInteractor: UserInteractor = UserInteractor()
) : UserPresenter(userInteractor) {

    override val loggingLifecycle = true

}
