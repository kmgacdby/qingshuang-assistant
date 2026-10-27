package com.qingshuang.assistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("qingshuang_settings", MODE_PRIVATE) }
    private lateinit var toggleButton: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(28, 36, 28, 24)
        }
        val title = TextView(this).apply { text = "清爽助手"; textSize = 30f; gravity = Gravity.CENTER }
        val subtitle = TextView(this).apply {
            text = "自动跳过辅助 · Android 10 及以上"
            textSize = 15f; gravity = Gravity.CENTER; setPadding(0, 10, 0, 24)
        }
        status = TextView(this).apply { textSize = 16f; setLineSpacing(7f, 1f) }
        toggleButton = Button(this).apply {
            setOnClickListener {
                val enabled = !prefs.getBoolean(KEY_ENABLED, true)
                prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
                refreshUi()
            }
        }
        val settingsButton = Button(this).apply {
            text = "打开无障碍设置"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val note = TextView(this).apply {
            text = "优先识别广告页中的跳过/关闭控件；文字节点不可点击时会尝试点击其区域。网络广告过滤尚未实现，后续将作为独立模块评估，可能与其他 VPN 冲突。"
            textSize = 13f; setPadding(0, 18, 0, 0)
        }
        fun add(view: android.view.View) {
            root.addView(view, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 })
        }
        add(title); add(subtitle); add(status); add(toggleButton); add(settingsButton); add(note)
        setContentView(root)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshUi()
    }

    private fun refreshUi() {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        status.text = "自动跳过：" + if (enabled) "已开启" else "已暂停" +
            "\n\n已针对开屏广告倒计时按钮优化。不同应用效果可能不同。"
        toggleButton.text = if (enabled) "暂停自动跳过" else "恢复自动跳过"
    }

    companion object { const val KEY_ENABLED = "auto_skip_enabled" }
}
