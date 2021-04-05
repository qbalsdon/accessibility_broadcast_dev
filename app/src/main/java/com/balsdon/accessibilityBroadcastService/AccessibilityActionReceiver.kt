package com.balsdon.AccessibilityDeveloperService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AccessibilityActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACCESSIBILITY_ACTION = "ACTION"

        const val ACTION_NEXT           = "NEXT_ELEMENT"
        const val ACTION_PREV           = "PREV_ELEMENT"

        const val ACTION_ELEMENT_TYPE_NEXT = "NEXT_ELEMENT_TYPE"
        const val ACTION_ELEMENT_TYPE_PREV = "PREV_ELEMENT_TYPE"
        const val ACTION_HEADING_NEXT   = "NEXT_HEADING"
        const val ACTION_HEADING_PREV   = "PREV_HEADING"
        const val ACTION_VOLUME_DOWN     = "VOLUME_UP"
        const val ACTION_VOLUME_UP      = "VOLUME_DOWN"
        const val ACTION_VOLUME_SET    = "VOLUME_SET"

        const val ACTION_WHICH          = "ACTION_WHICH"
        const val ACTION_DEBUG          = "ACTION_DEBUG"
        const val ACTION_MENU           = "ACTION_MENU"

        const val PARAMETER_VOLUME      = "PARAMETER_VOLUME"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        require(context != null) { "Context is required" }
        require(intent != null) { "Intent is required" }
        require(AccessibilityDeveloperService.instance != null) { "Service is required" }
        val serviceReference = AccessibilityDeveloperService.instance!!

        intent.getStringExtra(ACCESSIBILITY_ACTION)?.let {
            log("AccessibilityActionReceiver", "  ~~> PARAMETER: [$it]")
            serviceReference.apply {
                when (it) {
                    ACTION_DEBUG -> debugAction()
                    //TODO: Fix!
                    ACTION_MENU -> swipeUpRight()
                    ACTION_WHICH -> findFocusedViewInfo()
                    ACTION_PREV -> swipeHorizontal(false)
                    ACTION_VOLUME_SET -> setVolume(intent.getIntExtra(PARAMETER_VOLUME, 10))
                    ACTION_VOLUME_UP -> adjustVolume(true)
                    ACTION_VOLUME_DOWN -> adjustVolume(false)
                    ACTION_ELEMENT_TYPE_NEXT -> swipeVertical(true)
                    ACTION_ELEMENT_TYPE_PREV -> swipeVertical(false)
                    //default is just next
                    else -> swipeHorizontal(true)
                }
            }
        } ?: serviceReference.swipeHorizontal(true)
    }
}