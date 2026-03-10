package com.autoscript.ui.main.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.autoscript.base.BaseFragment
import com.autoscript.databinding.FragmentProfileBinding

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
    }

    override fun initData() {
    }
}
