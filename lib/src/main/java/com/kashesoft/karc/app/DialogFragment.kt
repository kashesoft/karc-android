/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karc.app

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.CallSuper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Router
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karc.utils.Logging
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Provider

abstract class DialogFragment<P : Presenter, out R : Router> : DaggerAppCompatDialogFragment(),
        Logging, Presentable, Routable {

    override val application: Application<*>
        get() =  activity?.application as Application<*>

    @Suppress("UNCHECKED_CAST")
    private val viewModel: ViewModel<P>
        get() = ViewModelProviders.of(this).get(ViewModel::class.java) as ViewModel<P>

    protected open val logging = false

    private fun log(message: String) {
        Log.v(name, ":::::::::::::::$message:::::::::::::::")
    }

    //region <==========|Lifecycle|==========>

    private var layoutIsCompleted = false

    protected open fun viewDidLoad() {}

    protected open fun viewDidLayout() {}

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (logging) log("onCreateView")
        return prepareView(inflater, container)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (logging) log("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
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
    }

    @CallSuper
    override fun onStop() {
        if (logging) log("onStop")
        super.onStop()
    }

    @CallSuper
    override fun onDestroyView() {
        if (logging) log("onDestroyView")
        super.onDestroyView()
        detachCompanionPresenter()
    }

    private fun prepareView(inflater: LayoutInflater, container: ViewGroup?): View {
        val viewable = this::class.annotations.find { it is Layout } as? Layout ?: throw IllegalStateException("Layout is not found!")
        val layoutResId = viewable.res
        return inflater.inflate(layoutResId, container, false)
    }

    private fun listenLayout() {
        view?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
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
        if (logging) log("viewDidLayout: (view.width = ${view!!.width}, view.height = ${view!!.height})")
        viewDidLayout()
        if (isResumed) {
            becomeActive()
        }
    }

    @CallSuper
    internal fun enterForeground() {
        presenter?.doEnterForeground()
    }

    @CallSuper
    private fun becomeActive() {
        presenter?.doBecomeActive()
    }

    @CallSuper
    private fun becomeInactive() {
        presenter?.doBecomeInactive()
    }

    @CallSuper
    internal fun enterBackground() {
        presenter?.doEnterBackground()
    }

    //endregion

    //region <==========|Presentation|==========>

    protected var presenter: P? = null
        private set
    protected open val presenterProvider: Provider<P>? = null

    private fun attachCompanionPresenter() {
        val presenter = viewModel.getPresenter()
        if (presenter == null) {
            val presenter = presenterProvider?.get() ?: return
            val params = router.paramsForComponent(this::class)
            presenter.attachPresentable(this)
            viewModel.setPresenter(presenter, params)
            this.presenter = presenter
        } else {
            presenter.attachPresentable(this)
            this.presenter = presenter
        }
    }

    private fun detachCompanionPresenter() {
        presenter?.detachPresentable(this)
        presenter = null
    }

    //endregion

    //region <==========|Routing|==========>

    protected abstract val router: R

    private fun attachCompanionRouter() {
        router.attachRoutable(this)
    }

    private fun detachCompanionRouter() {
        router.detachRoutable(this)
    }

    @CallSuper
    override fun route(query: Query): Boolean {
        return false
    }

    //endregion

}
