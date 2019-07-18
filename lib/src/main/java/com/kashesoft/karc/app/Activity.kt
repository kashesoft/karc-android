/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.kashesoft.karc.core.presenter.Presentable
import com.kashesoft.karc.core.presenter.Presenter
import com.kashesoft.karc.core.router.Query
import com.kashesoft.karc.core.router.Routable
import com.kashesoft.karc.core.router.Route
import com.kashesoft.karc.utils.Layout
import com.kashesoft.karc.utils.Logging
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private typealias KarcFragment = Fragment<*>
private typealias KarcDialogFragment = DialogFragment<*>

abstract class Activity<P : Presenter>(private val presenterClass: KClass<P>? = null) : AppCompatActivity(),
        Logging, Presentable, Routable {

    override val logging = true
    open val loggingLifecycle = false

    private fun log(message: String) {
        if (loggingLifecycle) logVerbose(":::::::::::::::$message:::::::::::::::")
    }

    @Suppress("UNCHECKED_CAST")
    private val viewModel: ViewModel<P>
        get() = ViewModelProviders.of(this).get(ViewModel::class.java) as ViewModel<P>

    protected val view: View
        get() = window.decorView.rootView

    //region <==========|Lifecycle|==========>

    var layoutIsCompleted = false
        private set

    protected open fun viewDidLoad() {}

    protected open fun viewDidLayout() {}

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        super.onCreate(savedInstanceState)
        prepareView()
        onLoad()
        listenLayout()
        attachCompanionPresenter()
    }

    @CallSuper
    override fun onStart() {
        log("onStart isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        super.onStart()
        viewModel.onStart()
    }

    @CallSuper
    override fun onResume() {
        log("onResume isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        attachCompanionRouter()
        super.onResume()
        viewModel.onResume()
    }

    @CallSuper
    override fun onPause() {
        log("onPause isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        viewModel.onPause(isChangingConfigurations)
        super.onPause()
        detachCompanionRouter()
    }

    @CallSuper
    override fun onStop() {
        log("onStop isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        viewModel.onStop(isChangingConfigurations)
        super.onStop()
    }

    @CallSuper
    override fun onDestroy() {
        log("onDestroy isChangingConfigurations()=$isChangingConfigurations, isFinishing=$isFinishing")
        super.onDestroy()
        detachCompanionPresenter()
    }

    protected open fun willShowFragmentInContainer(fragment: KarcFragment, @IdRes containerViewId: Int) {}

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
        log("viewDidLoad")
        viewDidLoad()
    }

    private fun onLayout() {
        layoutIsCompleted = true
        log("viewDidLayout: (view.width = ${view.width}, view.height = ${view.height})")
        viewDidLayout()
    }

    //endregion

    //region <==========|Presenter|==========>

    protected var presenter: P? = null
        private set

    private fun attachCompanionPresenter() {
        val presenterClass = presenterClass ?: return
        if (viewModel.hasNoPresenter(presenterClass)) {
            val params = Application.instance.router.paramsForComponent(this::class)
            viewModel.setPresenter(presenterClass, params)
        }
        val presenter = viewModel.getPresenter()!!
        presenter.attachPresentable(this)
        this.presenter = presenter
    }

    private fun detachCompanionPresenter() {
        presenter?.detachPresentable(this)
        presenter = null
    }

    //endregion

    //region <==========|Router|==========>

    private fun attachCompanionRouter() {
        Application.instance.router.attachRoutable(this)
    }

    internal fun detachCompanionRouter() {
        Application.instance.router.detachRoutable(this)
    }

    @CallSuper
    override fun route(query: Query): Boolean {
        return when (query.path) {
            Route.Path.FRAGMENT_SHOW_IN_CONTAINER -> {
                @Suppress("UNCHECKED_CAST")
                val fragmentClass: KClass<KarcFragment> = query.params[Route.Param.COMPONENT_CLASS] as KClass<KarcFragment>
                val fragmentContainer: Int = query.params[Route.Param.FRAGMENT_CONTAINER] as Int
                if (hasViewWithId(fragmentContainer)) {
                    showFragmentInContainer(
                            fragmentClass,
                            fragmentContainer,
                            android.R.animator.fade_in,
                            android.R.animator.fade_out
                    )
                    true
                } else {
                    false
                }
            }
            Route.Path.FRAGMENT_SHOW_AS_DIALOG -> {
                @Suppress("UNCHECKED_CAST")
                val fragmentClass: KClass<KarcDialogFragment> = query.params[Route.Param.COMPONENT_CLASS] as KClass<KarcDialogFragment>
                showFragmentAsDialog(fragmentClass)
                true
            }
            Route.Path.FRAGMENT_HIDE_AS_DIALOG -> {
                @Suppress("UNCHECKED_CAST")
                val fragmentClass: KClass<KarcDialogFragment> = query.params[Route.Param.COMPONENT_CLASS] as KClass<KarcDialogFragment>
                hideFragmentAsDialog(fragmentClass)
                true
            }
            else -> false
        }
    }

    private fun <T : KarcFragment> showFragmentInContainer(
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

        willShowFragmentInContainer(fragment, containerViewId)

        fragmentTransaction
                .setCustomAnimations(enterAnimation, exitAnimation)
                .replace(containerViewId, fragment, fragmentTag)
                .commitNowAllowingStateLoss()
    }

    private fun <T : KarcDialogFragment> showFragmentAsDialog(fragmentClass: KClass<T>) {
        val fragmentTag = fragmentClass.simpleName!!
        val currentFragmentWithTag = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (currentFragmentWithTag != null) return

        val fragment = fragmentClass.createInstance()

        fragment.show(supportFragmentManager, fragmentTag)
    }

    private fun <T : KarcDialogFragment> hideFragmentAsDialog(fragmentClass: KClass<T>) {
        val fragmentTag = fragmentClass.simpleName!!
        val currentFragmentWithTag = supportFragmentManager.findFragmentByTag(fragmentTag) as? KarcDialogFragment ?: return

        currentFragmentWithTag.dismissAllowingStateLoss()
    }

    private fun hasViewWithId(viewId: Int): Boolean {
        return findViewById<View>(viewId) != null
    }

    //endregion

}
