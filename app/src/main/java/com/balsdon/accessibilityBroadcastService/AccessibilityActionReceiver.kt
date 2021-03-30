package com.balsdon.accessibilityBroadcastService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AccessibilityActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACCESSIBILITY_ACTION = "ACTION"

        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_MENU = "ACTION_MENU"
        const val ACTION_WHAT = "ACTION_WHAT"
        const val ACTION_HEADING_NEXT = "ACTION_HEADING_NEXT"
        const val ACTION_HEADING_PREV = "ACTION_HEADING_PREV"
        const val ACTION_VOLUME_SET = "ACTION_VOLUME_SET"
        const val ACTION_VOLUME_UP = "ACTION_VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "ACTION_VOLUME_DOWN"
        const val PARAMETER_VOLUME = "PARAMETER_VOLUME"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        require(context != null) { "Context is required" }
        require(intent != null) { "Intent is required" }
        require(AccessibilityBroadcastService.instance != null) { "Service is required" }
        val serviceReference = AccessibilityBroadcastService.instance!!

        intent.getStringExtra(ACCESSIBILITY_ACTION)?.let {
            log("AccessibilityActionReceiver", "  ~~> PARAMETER: [$it]")
            serviceReference.apply {
                when (it) {
                    ACTION_MENU -> swipeUpLeft()
                    ACTION_WHAT -> findFocusedViewInfo()
                    ACTION_PREV -> swipeHorizontal(false)
                    ACTION_VOLUME_SET -> setVolume(intent.getIntExtra(PARAMETER_VOLUME, 10))
                    ACTION_VOLUME_UP -> adjustVolume(true)
                    ACTION_VOLUME_DOWN -> adjustVolume(false)
                    ACTION_HEADING_NEXT -> swipeVertical(true)
                    ACTION_HEADING_PREV -> swipeVertical(false)
                    //default is just next
                    else -> swipeHorizontal(true)
                }
            }
        } ?: serviceReference.swipeHorizontal(true)
    }
}