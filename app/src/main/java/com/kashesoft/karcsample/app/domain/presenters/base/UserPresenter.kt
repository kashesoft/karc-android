/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.presenters.base

import com.kashesoft.karc.core.interactor.Interaction
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karcsample.app.domain.entities.User
import com.kashesoft.karcsample.app.domain.interactors.UserInteractor

open class UserPresenter(
        private val userInteractor: UserInteractor
) : Presenter(userInteractor) {

    interface View : Presentable {
        fun showProgress()
        fun hideProgress()
        fun refreshProgress(percent: Int)
    }

    override val loggingLifecycle = true

    private val view
        get() = presentable(View::class)

    private var progress: Int = -1

    fun readUsers(userIds: List<Int>) {
        startProgress()
        userInteractor.loadUsers(userIds)
        userInteractor.loadUsers2(userIds)
    }

    fun badRecursion() {
        userInteractor.badRecursion()
    }

    override fun onBecomeActive() {
        if (inProgress()) {
            restartProgress()
        }
    }

    override fun onBecomeInactive() {
        if (inProgress()) {
            stopProgress()
        }
    }

    override fun onInteractionStart(interaction: Interaction<Any>) {
        startProgress()
    }

    override fun onInteractionSuccess(interaction: Interaction<Any>, data: Any) {
        val user = data as? User ?: return
        updateProgress(user.progress)
    }

    override fun onInteractionFinish(interaction: Interaction<Any>) {

    }

    override fun onInteractionFailure(interaction: Interaction<Any>, error: Throwable) {

    }

    override fun onInteractionCancel(interaction: Interaction<Any>) {

    }

    override fun onInteractionStop(interaction: Interaction<Any>) {
        finishProgress()
    }

    private fun startProgress() {
        progress = 0
        restartProgress()
    }

    private fun updateProgress(progress: Int) {
        this.progress = progress
        view?.refreshProgress(progress)
    }

    private fun stopProgress() {
        view?.hideProgress()
    }

    private fun restartProgress() {
        view?.showProgress()
        view?.refreshProgress(progress)
    }

    private fun finishProgress() {
        progress = -1
        view?.hideProgress()
    }

    private fun inProgress(): Boolean {
        return progress != -1
    }

}
