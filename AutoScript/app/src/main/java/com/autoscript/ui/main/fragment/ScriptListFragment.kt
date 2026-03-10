package com.autoscript.ui.main.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.autoscript.base.BaseFragment
import com.autoscript.databinding.FragmentScriptListBinding

class ScriptListFragment : BaseFragment<FragmentScriptListBinding>() {

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentScriptListBinding {
        return FragmentScriptListBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
    }

    override fun initData() {
    }

    override fun initListeners() {
    }
}
