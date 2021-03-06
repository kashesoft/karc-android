/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.user.fragments

import android.app.ProgressDialog
import android.os.Bundle
import com.kashesoft.karc.app.Fragment
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.domain.presenters.FirstPresenter
import com.kashesoft.karcsample.app.domain.presenters.base.UserPresenter
import kotlinx.android.synthetic.main.fragment_first.*

@Layout(res = R.layout.fragment_first)
class FirstFragment : Fragment<FirstPresenter>(FirstPresenter::class), UserPresenter.View {

    override val loggingLifecycle = true

    companion object {
        var cached: Any? = null
    }

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //cached = this
    }

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