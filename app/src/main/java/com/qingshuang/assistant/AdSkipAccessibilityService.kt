package com.qingshuang.assistant

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class AdSkipAccessibilityService : AccessibilityService() {
    private var lastClickAt = 0L
    private val cooldownMs = 2200L
    private val phrases = listOf(
        "跳过广告", "关闭广告", "跳过", "skip ad", "skip ads", "close ad", "close ads"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAt < cooldownMs) return

        val root = rootInActiveWindow ?: return
        try {
            if (findAndClick(root)) lastClickAt = now
        } finally {
            root.recycle()
        }
    }

    private fun findAndClick(node: AccessibilityNodeInfo): Boolean {
        val text = (node.text?.toString() ?: "").trim().lowercase(Locale.ROOT)
        val description = (node.contentDescription?.toString() ?: "").trim().lowercase(Locale.ROOT)
        val matches = phrases.any { phrase ->
            text == phrase.lowercase(Locale.ROOT) ||
                description == phrase.lowercase(Locale.ROOT)
        }

        if (matches && node.isVisibleToUser && node.isEnabled && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val clicked = try {
                findAndClick(child)
            } finally {
                child.recycle()
            }
            if (clicked) return true
        }
        return false
    }

    override fun onInterrupt() = Unit
}
