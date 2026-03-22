package com.jarvis.voice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    companion object { var instance: JarvisAccessibilityService? = null; var screenText: String = "" }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { super.onDestroy(); instance = null }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { rootInActiveWindow?.let { screenText = extractText(it) } }
    override fun onInterrupt() {}
    private fun extractText(node: AccessibilityNodeInfo, depth: Int = 0): String {
        if (depth > 8) return ""
        val sb = StringBuilder()
        if (!node.text.isNullOrBlank()) sb.append(node.text).append(" ")
        if (!node.contentDescription.isNullOrBlank()) sb.append(node.contentDescription).append(" ")
        for (i in 0 until node.childCount) { node.getChild(i)?.let { sb.append(extractText(it, depth+1)) } }
        return sb.toString()
    }
    fun clickOnText(text: String): Boolean { val node = rootInActiveWindow ?: return false; return findNodeByText(node,text)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false }
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.lowercase()?.contains(text.lowercase()) == true) return node
        if (node.contentDescription?.toString()?.lowercase()?.contains(text.lowercase()) == true) return node
        for (i in 0 until node.childCount) { val r = findNodeByText(node.getChild(i) ?: continue, text); if (r != null) return r }
        return null
    }
    fun typeText(text: String) { val node = rootInActiveWindow ?: return; val f = findFocusedInput(node) ?: return; val args = android.os.Bundle(); args.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text); f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) }
    private fun findFocusedInput(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) { val r = findFocusedInput(node.getChild(i) ?: continue); if (r != null) return r }
        return null
    }
    fun scrollDown() { rootInActiveWindow?.let { findScrollable(it)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) } }
    fun scrollUp() { rootInActiveWindow?.let { findScrollable(it)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) } }
    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) { val r = findScrollable(node.getChild(i) ?: continue); if (r != null) return r }
        return null
    }
    fun goBack() { performGlobalAction(GLOBAL_ACTION_BACK) }
    fun goHome() { performGlobalAction(GLOBAL_ACTION_HOME) }
}
