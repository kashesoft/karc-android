/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.user.activities

import android.support.design.widget.BottomNavigationView
import com.kashesoft.karc.app.Activity
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karcsample.R
import com.kashesoft.karcsample.app.domain.routers.MainRouter
import com.kashesoft.karcsample.app.user.fragments.FirstFragment
import com.kashesoft.karcsample.app.user.fragments.SecondFragment
import kotlinx.android.synthetic.main.activity_details.*
import javax.inject.Inject

@Layout(res = R.layout.activity_details)
class DetailsActivity : Activity<Nothing, MainRouter>() {

    @Inject override lateinit var router: MainRouter

    override fun viewDidLoad() {
        navigation.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_abc -> {
                    router.showFragmentInContainer(FirstFragment::class, R.id.container).route()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_xyz -> {
                    router.showFragmentInContainer(SecondFragment::class, R.id.container).route()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
    }

}
