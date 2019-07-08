/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.user.activities

import android.app.ProgressDialog
import com.kashesoft.karc.app.Activity
import com.kashesoft.karc.app.get
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.AppRouter
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
import com.kashesoft.karcsample.app.domain.presenters.AbcPresenter
import com.kashesoft.karcsample.app.domain.presenters.MainPresenter
import com.kashesoft.karcsample.app.domain.presenters.XyzPresenter
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter
import kotlinx.android.synthetic.main.activity_main.*

@Layout(res = R.layout.activity_main)
class MainActivity : Activity<MainPresenter>(MainPresenter::class), UserPresenter.View {

    override val loggingLifecycle = true

    private lateinit var progressDialog: ProgressDialog

    override fun viewDidLoad() {
        //
        setUpAbcGatewayButton.setOnClickListener {
            AppRouter::class.get.setUpGateway(AbcGateway::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownAbcGatewayButton.setOnClickListener {
            AppRouter::class.get.tearDownGateway(AbcGateway::class).route()
        }
        //
        setUpXyzGatewayButton.setOnClickListener {
            AppRouter::class.get.setUpGateway(XyzGateway::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownXyzGatewayButton.setOnClickListener {
            AppRouter::class.get.tearDownGateway(XyzGateway::class).route()
        }
        //
        setUpAbcPresenterButton.setOnClickListener {
            AppRouter::class.get.setUpPresenter(AbcPresenter::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownAbcPresenterButton.setOnClickListener {
            AppRouter::class.get.tearDownPresenter(AbcPresenter::class).route()
        }
        //
        setUpXyzPresenterButton.setOnClickListener {
            AppRouter::class.get.setUpPresenter(XyzPresenter::class, mapOf("string" to "test string")).route()
        }
        //
        tearDownXyzPresenterButton.setOnClickListener {
            AppRouter::class.get.tearDownPresenter(XyzPresenter::class).route()
        }
        //
        nextButton.setOnClickListener {
            AppRouter::class.get.startActivity(
                    DetailsActivity::class,
                    mapOf("string" to "test string", "number" to 123)
            ).route()
        }
        //
        button.setOnClickListener {
            val userIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            presenter?.readUsers(userIds)
        }
        button2.setOnClickListener {
            presenter?.badRecursion()
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
