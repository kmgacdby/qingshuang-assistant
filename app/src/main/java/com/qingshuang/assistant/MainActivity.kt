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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "清爽助手"
            textSize = 30f
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "非 Root · Android 10 及以上"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 24)
        }
        val status = TextView(this).apply {
            text = "当前版本：0.1.0 预览版\n\n目前仅提供明确广告跳过按钮的辅助点击原型。\n\n广告域名过滤、规则更新和应用白名单尚未实现。"
            textSize = 17f
            setLineSpacing(8f, 1f)
        }
        val settingsButton = Button(this).apply {
            text = "开启无障碍辅助"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        val note = TextView(this).apply {
            text = "启用后，系统可能允许本服务读取屏幕控件文字并点击匹配按钮。请仅在了解其权限范围后开启。"
            textSize = 13f
            setPadding(0, 18, 0, 0)
        }

        root.addView(title, matchWidthWrap())
        root.addView(subtitle, matchWidthWrap())
        root.addView(status, matchWidthWrap())
        root.addView(settingsButton, matchWidthWrap())
        root.addView(note, matchWidthWrap())
        setContentView(root)
    }

    private fun matchWidthWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = 12 }
}
