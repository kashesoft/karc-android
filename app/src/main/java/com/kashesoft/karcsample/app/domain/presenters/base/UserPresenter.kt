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

    override val logging = true

    private val view
        get() = presentable(View::class)

    private var progress: Int = -1

    fun readUsers(userIds: List<Int>) {
        startProgress()
        userInteractor.loadUsers(userIds)
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

    override fun onInteractionStarted(interaction: Interaction<*>) {
        startProgress()
    }

    override fun onInteractionNext(interaction: Interaction<*>, data: Any) {
        val user = data as? User ?: return
        updateProgress(user.progress)
    }

    override fun onInteractionCompleted(interaction: Interaction<*>) {

    }

    override fun onInteractionError(interaction: Interaction<*>, error: Throwable) {

    }

    override fun onInteractionDisposed(interaction: Interaction<*>) {

    }

    override fun onInteractionStopped(interaction: Interaction<*>) {
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
