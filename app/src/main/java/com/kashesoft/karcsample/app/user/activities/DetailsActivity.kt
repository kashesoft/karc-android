/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.user.activities

import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast
import com.kashesoft.karc.app.Activity
import com.kashesoft.karc.app.get
import com.kashesoft.karc.app.getOrNull
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.AppRouter
import com.kashesoft.karcsample.app.domain.gateways.AbcGateway
import com.kashesoft.karcsample.app.domain.gateways.XyzGateway
import com.kashesoft.karcsample.app.domain.presenters.AbcPresenter
import com.kashesoft.karcsample.app.domain.presenters.XyzPresenter
import com.kashesoft.karcsample.app.user.fragments.FirstFragment
import com.kashesoft.karcsample.app.user.fragments.SecondFragment
import kotlinx.android.synthetic.main.activity_details.*

@Layout(res = R.layout.activity_details)
class DetailsActivity : Activity<Nothing>() {

    override fun viewDidLoad() {
        navigation.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_abc -> {
                    AppRouter::class.get.showFragmentInContainer(FirstFragment::class, R.id.container).route()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_xyz -> {
                    AppRouter::class.get.showFragmentInContainer(SecondFragment::class, R.id.container).route()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
        AbcGateway::class.getOrNull?.let {
            Toast.makeText(this, "Detected $it", Toast.LENGTH_SHORT).show()
        }
        XyzGateway::class.getOrNull?.let {
            Toast.makeText(this, "Detected $it", Toast.LENGTH_SHORT).show()
        }
        AbcPresenter::class.getOrNull?.let {
            Toast.makeText(this, "Detected $it", Toast.LENGTH_SHORT).show()
        }
        XyzPresenter::class.getOrNull?.let {
            Toast.makeText(this, "Detected $it", Toast.LENGTH_SHORT).show()
        }

    }

}
