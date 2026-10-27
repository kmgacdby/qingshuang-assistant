package com.qingshuang.assistant

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Best-effort helper for explicit ad skip/close controls.
 * It does not block video ads; it only attempts to activate a visible UI control.
 */
class AdSkipAccessibilityService : AccessibilityService() {
    private var lastClickAt = 0L
    private var lastScanAt = 0L
    private val clickCooldownMs = 1800L
    private val scanIntervalMs = 350L

    private val explicitAdSkipPhrases = listOf(
        "跳过广告", "跳过此广告", "关闭广告", "关闭此广告",
        "skip ad", "skip ads", "close ad", "close ads",
        "dismiss ad", "skip advertisement", "close advertisement"
    )
    private val genericSkipPhrases = listOf("跳过", "skip")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> Unit
            else -> return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastScanAt < scanIntervalMs || now - lastClickAt < clickCooldownMs) return
        lastScanAt = now

        val root = rootInActiveWindow ?: return
        try {
            // Avoid clicking ordinary "Skip" controls unless the current screen also
            // exposes an explicit ad/sponsored marker.
            val hasAdMarker = containsAdMarker(root)
            if (findAndClick(root, hasAdMarker)) lastClickAt = now
        } finally {
            root.recycle()
        }
    }

    private fun normalizedLabels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

    private fun containsAdMarker(node: AccessibilityNodeInfo): Boolean {
        if (normalizedLabels(node).any { label ->
                label.contains("广告") ||
                    label.contains("赞助内容") ||
                    Regex("\\b(ad|ads|advertisement|sponsored)\\b").containsMatchIn(label)
            }) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try {
                containsAdMarker(child)
            } finally {
                child.recycle()
            }
            if (found) return true
        }
        return false
    }

    private fun findAndClick(node: AccessibilityNodeInfo, hasAdMarker: Boolean): Boolean {
        val labels = normalizedLabels(node)
        val explicitMatch = labels.any { label ->
            explicitAdSkipPhrases.any { phrase -> label.contains(phrase) }
        }
        val genericMatch = hasAdMarker && labels.any { label ->
            genericSkipPhrases.any { phrase -> label == phrase }
        }

        if ((explicitMatch || genericMatch) && node.isVisibleToUser) {
            if (clickNodeOrClickableAncestor(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val clicked = try {
                findAndClick(child, hasAdMarker)
            } finally {
                child.recycle()
            }
            if (clicked) return true
        }
        return false
    }

    private fun clickNodeOrClickableAncestor(start: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(start)
        var depth = 0
        try {
            while (current != null && depth < 6) {
                if (current.isVisibleToUser && current.isEnabled && current.isClickable) {
                    if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
                val parent = current.parent
                current.recycle()
                current = parent
                depth++
            }
        } finally {
            current?.recycle()
        }
        return false
    }

    override fun onInterrupt() = Unit
}
