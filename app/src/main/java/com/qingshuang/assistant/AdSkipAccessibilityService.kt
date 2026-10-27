package com.qingshuang.assistant

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import java.util.Locale

/**
 * Best-effort helper for visible ad skip controls.
 * It cannot block an ad; it only activates a likely skip control when one is exposed.
 */
class AdSkipAccessibilityService : AccessibilityService() {
    private var lastActionAt = 0L
    private var lastScanAt = 0L
    private val actionCooldownMs = 1800L
    private val scanIntervalMs = 250L

    private val explicitAdPhrases = listOf("广告", "赞助", "advertisement", "sponsored")
    private val explicitSkipPhrases = listOf(
        "跳过广告", "跳过此广告", "关闭广告", "关闭此广告",
        "skip ad", "skip ads", "close ad", "close ads",
        "dismiss ad", "skip advertisement", "close advertisement"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> Unit
            else -> return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastScanAt < scanIntervalMs || now - lastActionAt < actionCooldownMs) return
        lastScanAt = now

        val root = rootInActiveWindow ?: return
        try {
            val hasAdMarker = containsAdMarker(root)
            if (findAndActivate(root, hasAdMarker)) lastActionAt = now
        } finally {
            root.recycle()
        }
    }

    private fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

    private fun containsAdMarker(node: AccessibilityNodeInfo): Boolean {
        if (labels(node).any { label ->
                explicitAdPhrases.any { label.contains(it) } ||
                    label.contains("互动广告")
            }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try { containsAdMarker(child) } finally { child.recycle() }
            if (found) return true
        }
        return false
    }

    private fun isSkipLabel(label: String, hasAdMarker: Boolean): Boolean {
        if (explicitSkipPhrases.any { label.contains(it) }) return true
        if (!hasAdMarker) return false
        // Supports labels such as "1 跳过", "跳过", "Skip in 1", or "1 Skip".
        return label.contains("跳过") ||
            Regex("\\bskip\\b").containsMatchIn(label)
    }

    private fun findAndActivate(node: AccessibilityNodeInfo, hasAdMarker: Boolean): Boolean {
        val matchingLabel = labels(node).any { isSkipLabel(it, hasAdMarker) }
        if (matchingLabel && node.isVisibleToUser) {
            if (clickNodeOrClickableAncestor(node)) return true
            // Some splash ads expose the label as a non-clickable text node. If its
            // bounds are valid and the page has an ad marker, tap its center as fallback.
            if (hasAdMarker && dispatchTapAtNodeCenter(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val activated = try {
                findAndActivate(child, hasAdMarker)
            } finally {
                child.recycle()
            }
            if (activated) return true
        }
        return false
    }

    private fun clickNodeOrClickableAncestor(start: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(start)
        var depth = 0
        try {
            while (current != null && depth < 6) {
                if (current.isVisibleToUser && current.isEnabled && current.isClickable &&
                    current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
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

    private fun dispatchTapAtNodeCenter(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty || rect.width() < 8 || rect.height() < 8) return false
        val path = Path().apply { moveTo(rect.exactCenterX(), rect.exactCenterY()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() = Unit
}
