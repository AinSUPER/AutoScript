package com.autoscript.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStatePagerAdapter
import com.autoscript.ui.main.fragment.LogFragment
import com.autoscript.ui.main.fragment.ProfileFragment
import com.autoscript.ui.main.fragment.ScriptListFragment

class MainPagerAdapter(activity: FragmentActivity) : 
    FragmentStatePagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ScriptListFragment()
            1 -> LogFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
