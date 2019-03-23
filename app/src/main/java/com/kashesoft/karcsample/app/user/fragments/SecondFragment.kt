/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.user.fragments

import android.app.ProgressDialog
import com.kashesoft.karc.app.Fragment
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karc.utils.Provider
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.domain.presenters.SecondPresenter
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter
import kotlinx.android.synthetic.main.fragment_second.*

@Layout(res = R.layout.fragment_second)
class SecondFragment : Fragment<SecondPresenter>(), UserPresenter.View {

    override val presenterProvider = Provider<SecondPresenter> { SecondPresenter() }

    private lateinit var progressDialog: ProgressDialog

    override fun viewDidLoad() {
        //
        button.setOnClickListener {
            val userIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            presenter?.readUsers(userIds)
        }
        //
        val progressDialog = ProgressDialog(context)
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