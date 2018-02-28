/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.user.activities

import android.app.ProgressDialog
import com.kashesoft.karc.app.Activity
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.data.controllers.AbcController
import com.kashesoft.karcsample.app.data.controllers.XyzController
import com.kashesoft.karcsample.app.domain.presenters.AbcPresenter
import com.kashesoft.karcsample.app.domain.presenters.MainPresenter
import com.kashesoft.karcsample.app.domain.presenters.XyzPresenter
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter
import com.kashesoft.karcsample.app.domain.routers.MainRouter
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject
import javax.inject.Provider

@Layout(res = R.layout.activity_main)
class MainActivity : Activity<MainPresenter, MainRouter>(), UserPresenter.View {

    @Inject override lateinit var presenterProvider: Provider<MainPresenter>

    @Inject override lateinit var router: MainRouter

    private lateinit var progressDialog: ProgressDialog

    override fun viewDidLoad() {
        //
        setUpAbcControllerButton.setOnClickListener {
            router.setUpController(AbcController::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownAbcControllerButton.setOnClickListener {
            router.tearDownController(AbcController::class).route()
        }
        //
        setUpXyzControllerButton.setOnClickListener {
            router.setUpController(XyzController::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownXyzControllerButton.setOnClickListener {
            router.tearDownController(XyzController::class).route()
        }
        //
        setUpAbcPresenterButton.setOnClickListener {
            router.setUpPresenter(AbcPresenter::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownAbcPresenterButton.setOnClickListener {
            router.tearDownPresenter(AbcPresenter::class).route()
        }
        //
        setUpXyzPresenterButton.setOnClickListener {
            router.setUpPresenter(XyzPresenter::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownXyzPresenterButton.setOnClickListener {
            router.tearDownPresenter(XyzPresenter::class).route()
        }
        //
        nextButton.setOnClickListener {
            router.showActivity(
                    DetailsActivity::class,
                    mapOf("string" to "test string", "number" to 123)
            ).route()
        }
        //
        button.setOnClickListener {
            val userIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            presenter?.readUsers(userIds)
        }
        //
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(true)
        progressDialog.setOnCancelListener { presenter?.disposeInteractions() }
        this.progressDialog = progressDialog
    }

    override fun showProgress() {
        progressDialog.show()
    }

    override fun hideProgress() {
        progressDialog.dismiss()
    }

    override fun refreshProgress(percent: Int) {
        progressDialog.progress = percent
    }

}
