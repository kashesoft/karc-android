/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.AnimRes
import android.support.annotation.AnimatorRes
import android.support.annotation.CallSuper
import android.support.annotation.IdRes
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.core.router.Router
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karc.utils.Logging
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Provider
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

abstract class Activity<P : Presenter, out R : Router> : DaggerAppCompatActivity(),
        Logging, Presentable, Routable {

    override val application: Application<*>
        get() = applicationContext as Application<*>

    @Suppress("UNCHECKED_CAST")
    private val viewModel: ViewModel<P>
        get() = ViewModelProviders.of(this).get(ViewModel::class.java) as ViewModel<P>

    protected val view: View
        get() = window.decorView.rootView

    protected open val logging = false

    private fun log(message: String) {
        Log.v(name, ":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    private var layoutIsCompleted = false
    private var isResumed = false

    protected open fun viewDidLoad() {}

    protected open fun viewDidLayout() {}

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        if (logging) log("onCreate")
        super.onCreate(savedInstanceState)
        prepareView()
        onLoad()
        listenLayout()
        attachCompanionPresenter()
    }

    @CallSuper
    override fun onStart() {
        if (logging) log("onStart")
        super.onStart()
    }

    @CallSuper
    override fun onResume() {
        if (logging) log("onResume")
        isResumed = true
        super.onResume()
        attachCompanionRouter()
        if (layoutIsCompleted) {
            becomeActive()
        }
    }

    @CallSuper
    override fun onPause() {
        if (logging) log("onPause")
        becomeInactive()
        detachCompanionRouter()
        super.onPause()
        isResumed = false
    }

    @CallSuper
    override fun onStop() {
        if (logging) log("onStop")
        super.onStop()
    }

    @CallSuper
    override fun onDestroy() {
        if (logging) log("onDestroy")
        super.onDestroy()
        detachCompanionPresenter()
    }

    private fun prepareView() {
        val viewable = this::class.annotations.find { it is Layout } as? Layout ?: throw IllegalStateException("Layout is not found!")
        val layoutResId = viewable.res
        setContentView(layoutResId)
    }

    private fun listenLayout() {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onLayout()
            }
        })
    }

    private fun onLoad() {
        if (logging) log("viewDidLoad")
        viewDidLoad()
    }

    private fun onLayout() {
        layoutIsCompleted = true
        if (logging) log("viewDidLayout: (view.width = ${view.width}, view.height = ${view.height})")
        viewDidLayout()
        if (isResumed) {
            becomeActive()
        }
    }

    internal fun enterForeground() {
        presenter?.doEnterForeground()
        supportFragmentManager.fragments.forEach {
            val fragment = it as? Fragment<*, *> ?: return@forEach
            fragment.enterForeground()
        }
    }

    private fun becomeActive() {
        presenter?.doBecomeActive()
    }

    private fun becomeInactive() {
        presenter?.doBecomeInactive()
    }

    internal fun enterBackground() {
        presenter?.doEnterBackground()
        supportFragmentManager.fragments.forEach {
            val fragment = it as? Fragment<*, *> ?: return@forEach
            fragment.enterBackground()
        }
    }

    //endregion

    //region <==========|Presenter|==========>

    protected var presenter: P? = null
        private set
    protected open val presenterProvider: Provider<P>? = null

    private fun attachCompanionPresenter() {
        if (viewModel.getPresenter() == null) {
            val presenter = presenterProvider?.get() ?: return
            val params = router.paramsForComponent(this::class)
            presenter.attachPresentable(this)
            viewModel.setPresenter(presenter, params)
            this.presenter = presenter
        } else {
            this.presenter = viewModel.getPresenter()!!
            this.presenter?.attachPresentable(this)
        }
    }

    private fun detachCompanionPresenter() {
        presenter?.detachPresentable(this)
        presenter = null
    }

    //endregion

    //region <==========|Router|==========>

    protected abstract val router: R

    private fun attachCompanionRouter() {
        router.attachRoutable(this)
    }

    private fun detachCompanionRouter() {
        router.detachRoutable(this)
    }

    @CallSuper
    override fun route(query: Query): Boolean {
        return when (query.path) {
            Route.Path.FRAGMENT_SHOW_IN_CONTAINER -> {
                @Suppress("UNCHECKED_CAST")
                val fragmentClass: KClass<Fragment<*, *>> = query.params[Route.Param.COMPONENT_CLASS] as KClass<Fragment<*, *>>
                val fragmentContainer: Int = query.params[Route.Param.FRAGMENT_CONTAINER] as Int
                showFragmentInContainer(
                        fragmentClass,
                        fragmentContainer,
                        android.R.animator.fade_in,
                        android.R.animator.fade_out
                )
                true
            }
            else -> false
        }
    }

    private fun <T : Fragment<*, *>> showFragmentInContainer(
            fragmentClass: KClass<T>,
            @IdRes containerViewId: Int,
            @AnimatorRes @AnimRes enterAnimation: Int,
            @AnimatorRes @AnimRes exitAnimation: Int
    ) {
        val fragmentTag = fragmentClass.simpleName!!
        val currentFragmentWithTag = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (currentFragmentWithTag != null) return

        val fragment = fragmentClass.createInstance()

        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Remove previous fragment in container
        supportFragmentManager.fragments
                .filter { it.activity == this && (it.view?.parent as? ViewGroup)?.id == containerViewId }
                .forEach { fragmentTransaction.remove(it) }

        fragmentTransaction
                .setCustomAnimations(enterAnimation, exitAnimation)
                .replace(containerViewId, fragment, fragmentTag)
                .commitNowAllowingStateLoss()
    }

    //endregion

}
