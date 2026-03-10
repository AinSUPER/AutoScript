package com.autoscript.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.autoscript.utils.LogUtils
import com.autoscript.utils.ToastUtils

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initData()
        initListeners()
        observeData()

        LogUtils.d(this::class.simpleName, "onViewCreated")
    }

    protected open fun initViews() {}

    protected open fun initData() {}

    protected open fun initListeners() {}

    protected open fun observeData() {}

    protected fun showToast(message: String) {
        ToastUtils.show(requireContext(), message)
    }

    protected fun showToast(resId: Int) {
        ToastUtils.show(requireContext(), resId)
    }

    protected fun requireBaseActivity(): BaseActivity<*> {
        return requireActivity() as BaseActivity<*>
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        LogUtils.d(this::class.simpleName, "onDestroyView")
    }
}
