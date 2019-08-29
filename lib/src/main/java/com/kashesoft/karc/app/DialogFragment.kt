/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karc.utils.Logging
import com.kashesoft.karc.utils.profileObjectDidCreate
import com.kashesoft.karc.utils.profileObjectWillDestroy
import kotlin.reflect.KClass

abstract class DialogFragment<P : Presenter>(private val presenterClass: KClass<P>? = null) : DialogFragment(),
        Logging, Presentable, Routable {

    override val logging = true
    open val loggingLifecycle = false

    private fun log(message: String) {
        if (loggingLifecycle) logVerbose(":::::::::::::::$message:::::::::::::::")
    }

    @Suppress("UNCHECKED_CAST")
    private val viewModel: ViewModel<P>
        get() = ViewModelProviders.of(this).get(ViewModel::class.java) as ViewModel<P>

    //region <==========|Lifecycle|==========>

    var layoutIsCompleted = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")
        if (Application.instance.autoObjectProfiling) profileObjectDidCreate()
        super.onCreate(savedInstanceState)
    }

    protected open fun viewDidLoad() {}

    protected open fun viewDidLayout() {}

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        log("onCreateView")
        return prepareView(inflater, container)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        log("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        onLoad()
        listenLayout()
        attachCompanionPresenter()
    }

    @CallSuper
    override fun onStart() {
        log("onStart")
        super.onStart()
        viewModel.onStart()
    }

    @CallSuper
    override fun onResume() {
        log("onResume")
        attachCompanionRouter()
        super.onResume()
        viewModel.onResume()
    }

    @CallSuper
    override fun onPause() {
        log("onPause")
        viewModel.onPause(activity?.isChangingConfigurations ?: false)
        super.onPause()
        detachCompanionRouter()
    }

    @CallSuper
    override fun onStop() {
        log("onStop")
        viewModel.onStop(activity?.isChangingConfigurations ?: false)
        super.onStop()
    }

    @CallSuper
    override fun onDestroyView() {
        log("onDestroyView")
        super.onDestroyView()
        detachCompanionPresenter()
    }

    override fun onDestroy() {
        log("onDestroy")
        super.onDestroy()
        if (Application.instance.autoObjectProfiling) profileObjectWillDestroy()
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
        log("viewDidLoad")
        viewDidLoad()
    }

    private fun onLayout() {
        layoutIsCompleted = true
        log("viewDidLayout: (view.width = ${view?.width}, view.height = ${view?.height})")
        viewDidLayout()
    }

    //endregion

    //region <==========|Presentation|==========>

    protected var presenter: P? = null
        private set

    private fun attachCompanionPresenter() {
        val presenterClass = presenterClass ?: return
        val presenter = viewModel.getPresenter()
        if (presenter == null) {
            val componentTag = arguments?.getString(Route.Param.COMPONENT_TAG) ?: "default"
            val params = Application.instance.router.paramsForComponent(this::class, componentTag)
            viewModel.setPresenter(presenterClass, componentTag, params)
            val presenter = viewModel.getPresenter()!!
            presenter.attachPresentable(this)
            viewModel.onCreate()
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

    private fun attachCompanionRouter() {
        Application.instance.router.attachRoutable(this)
    }

    private fun detachCompanionRouter() {
        Application.instance.router.detachRoutable(this)
    }

    @CallSuper
    override fun route(query: Query): Boolean {
        return false
    }

    //endregion

}
