package com.autoscript.ui.main.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.autoscript.base.BaseFragment
import com.autoscript.databinding.FragmentLogBinding

class LogFragment : BaseFragment<FragmentLogBinding>() {

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLogBinding {
        return FragmentLogBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
    }

    override fun initData() {
    }
}
