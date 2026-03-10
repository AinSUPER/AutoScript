package com.autoscript.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.autoscript.base.BaseActivity
import com.autoscript.databinding.ActivityMainBinding
import com.autoscript.service.AccessibilityServiceManager
import com.autoscript.ui.permission.PermissionGuideActivity
import com.autoscript.ui.script.ScriptEditActivity
import com.autoscript.ui.settings.SettingsActivity
import com.autoscript.utils.PermissionUtils
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var accessibilityManager: AccessibilityServiceManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    override fun createBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessibilityManager = AccessibilityServiceManager(this)
    }

    override fun initViews() {
        setupViewPager()
        setupToolbar()
    }

    override fun initData() {
        checkPermissions()
    }

    override fun initListeners() {
        binding.fabAddScript.setOnClickListener {
            startActivity(Intent(this, ScriptEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "脚本"
                1 -> "日志"
                2 -> "我的"
                else -> ""
            }
        }.attach()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                com.autoscript.R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                com.autoscript.R.id.action_permission -> {
                    startActivity(Intent(this, PermissionGuideActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun checkPermissions() {
        lifecycleScope.launch {
            val hasOverlay = Settings.canDrawOverlays(this@MainActivity)
            val hasAccessibility = accessibilityManager.isAccessibilityServiceEnabled()
            val hasStorage = PermissionUtils.hasStoragePermission(this@MainActivity)

            if (!hasOverlay || !hasAccessibility || !hasStorage) {
                showPermissionGuide()
            }
        }
    }

    private fun showPermissionGuide() {
        startActivity(Intent(this, PermissionGuideActivity::class.java))
    }
}
