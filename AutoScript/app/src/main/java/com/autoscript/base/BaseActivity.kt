package com.autoscript.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.autoscript.utils.LogUtils
import com.autoscript.utils.ToastUtils

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected abstract fun createBinding(inflater: LayoutInflater): VB

    protected open val enableBackPressedCallback: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = createBinding(layoutInflater)
        setContentView(binding.root)

        if (enableBackPressedCallback) {
            setupBackPressedCallback()
        }

        initViews()
        initData()
        initListeners()
        observeData()

        LogUtils.d(this::class.simpleName, "onCreate")
    }

    protected open fun initViews() {}

    protected open fun initData() {}

    protected open fun initListeners() {}

    protected open fun observeData() {}

    protected open fun onBackPressHandled(): Boolean = false

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!onBackPressHandled()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    protected fun showToast(message: String) {
        ToastUtils.show(this, message)
    }

    protected fun showToast(resId: Int) {
        ToastUtils.show(this, resId)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        LogUtils.d(this::class.simpleName, "onDestroy")
    }
}
