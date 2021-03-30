package com.balsdon.accessibilityBroadcastService

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Path
import android.media.AudioManager
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.get

class AccessibilityBroadcastService : AccessibilityService() {
    companion object {
        //TODO: [1] Not a huge fan of this...
        //https://developer.android.com/reference/android/content/BroadcastReceiver#peekService(android.content.Context,%20android.content.Intent)
        var instance: AccessibilityBroadcastService? = null
    }

    //REQUIRED overrides... not used
    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    private val accessibilityActionReceiver = AccessibilityActionReceiver()

    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    private val displayMetrics : DisplayMetrics by lazy { Resources.getSystem().displayMetrics }
    private val halfWidth      : Float by lazy { (displayMetrics.widthPixels) / 2f }
    private val halfHeight     : Float by lazy { (displayMetrics.heightPixels) / 2f }
    private val quarterWidth   : Float by lazy { halfWidth / 2f }
    private val quarterHeight  : Float by lazy { halfWidth / 2f }

    fun findFocusedViewInfo(): AccessibilityNodeInfo = with(rootInActiveWindow) {
        val viewInfo = this.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        log(
            "AccessibilityBroadcastService",
            "  ~~> View in focus: [${viewInfo.className} : ${viewInfo.viewIdResourceName}]"
        )
        return viewInfo
    }

    override fun onServiceConnected() {
        log(
            "AccessibilityBroadcastService",
            "onServiceConnected"
        )
        registerReceiver(accessibilityActionReceiver, IntentFilter().apply {
            addAction(ACCESSIBILITY_CONTROL_BROADCAST_ACTION)
            priority = 100
            log(
                "AccessibilityBroadcastService",
                "    ~~> Receiver is registered."
            )
        })
        instance = this
    }

    fun swipeHorizontal(leftToRight: Boolean) {
        val swipePath = Path()
        if (leftToRight) {
            swipePath.moveTo(halfWidth - quarterWidth, halfHeight)
            swipePath.lineTo(halfWidth + quarterWidth, halfHeight)
        } else {
            swipePath.moveTo(halfWidth + quarterWidth, halfHeight)
            swipePath.lineTo(halfWidth - quarterWidth, halfHeight)
        }
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 500))
        dispatchGesture(gestureBuilder.build(), GestureResultCallback(baseContext), null)
    }

    fun swipeVertical(downToUp: Boolean = true) {
        val swipePath = Path().apply {
            if (downToUp) {
                moveTo(halfWidth - quarterWidth, halfHeight - quarterHeight)
                lineTo(halfWidth - quarterWidth, halfHeight + quarterHeight)
            } else {
                moveTo(halfWidth - quarterWidth, halfHeight + quarterHeight)
                lineTo(halfWidth - quarterWidth, halfHeight - quarterHeight)
            }
        }

        performGesture(
            GestureDescription
                .Builder()
                .addStroke(
                    StrokeDescription(swipePath, 0, 500)
                )
                .build()
        )
    }

    //https://developer.android.com/guide/topics/ui/accessibility/service#continued-gestures
    //TODO: Fix this
    fun swipeUpLeft() {
        val stX = halfWidth + quarterWidth
        val enX = halfWidth - quarterWidth

        val stY = halfHeight + quarterHeight
        val enY = halfHeight - quarterHeight

        val swipeUp = Path().apply {
            moveTo(stX, stY)
            lineTo(stX, enY)
        }
        val swipeLeftToRight = Path().apply {
            moveTo(stX, enY)
            lineTo(enX, enY)
        }

        log("path_v", "to x: [$stX], y: [$stY]")
        log("path_v", "mv x: [$stX], y: [$enY]")
        log("path_v", "dl x: [${stX - stX}], y: [${enY - stY}]")

        log("path_h", "to x: [$stX], y: [$enY]")
        log("path_h", "mv x: [$enX], y: [$enY]")
        log("path_h", "dl x: [${enX - stX}], y: [${enY - enY}]")

        val duration = 500L
        performGesture(GestureDescription.Builder().addStroke(
            StrokeDescription(
                swipeUp,
                0L,
                duration,
                true
            ).apply {
                continueStroke(
                    swipeLeftToRight,
                    duration,
                    duration,
                    false
                )
            }
        ).build())
    }

    // https://developer.android.com/guide/topics/ui/accessibility/service#continued-gestures
    // Taken from online documentation. Seems to have left out the dispatch of the gesture.
    // Also does not seem to be an accessibility gesture, but a "regular" gesture (I'm not sure)
    // Simulates an L-shaped drag path: 200 pixels right, then 200 pixels down.
    private fun doRightThenDownDrag() {
        val dragRightPath = Path().apply {
            moveTo(200f, 200f)
            lineTo(400f, 200f)
        }
        val dragRightDuration = 500L // 0.5 second

        // The starting point of the second path must match
        // the ending point of the first path.
        val dragDownPath = Path().apply {
            moveTo(400f, 200f)
            lineTo(400f, 400f)
        }
        val dragDownDuration = 500L
        val rightThenDownDrag = StrokeDescription(
            dragRightPath,
            0L,
            dragRightDuration,
            true
        ).apply {
            continueStroke(dragDownPath, dragRightDuration, dragDownDuration, false)
        }
    }

    private fun performGesture(gestureDescription: GestureDescription) =
        dispatchGesture(gestureDescription, GestureResultCallback(baseContext), null)


    class GestureResultCallback(private val ctx: Context) :
        AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            log("GestureResultCallback", "DONE SWIPE")
            super.onCompleted(gestureDescription)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            log("GestureResultCallback", "DIDN'T SWIPE")
            super.onCancelled(gestureDescription)
        }
    }

    // default to lower in case you forget
    // because everyone LOVES accessibility over VC and in the [home] office
    /*
    TODO: Allow for
       muting /
       maxing /
       setting to a distinct percentage value
       one above min
    */
    fun adjustVolume(raise: Boolean = false) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_ACCESSIBILITY,
            if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun setVolume(percent: Int) {
        require(percent <= 100) { " percent must be an integer less than 100" }
        require(percent >= 0) { " percent must be an integer greater than 0" }
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY)
        val volume = (max * (percent.toFloat() / 100f)).toInt()
        log("AccessibilityBroadcastService", "  ~~> Volume set to value [$volume]")
        audioManager.setStreamVolume(
            AudioManager.STREAM_ACCESSIBILITY,
            volume,
            AudioManager.FLAG_SHOW_UI
        )
    }

    override fun onDestroy() {
        log(
            "AccessibilityBroadcastService",
            "  ~~> onDestroy"
        )
        // Unregister accessibilityActionReceiver when destroyed.
        // I have had bad luck with broadcast receivers in the past
        try {
            unregisterReceiver(accessibilityActionReceiver)
            log(
                "AccessibilityBroadcastService",
                "    ~~> Receiver is unregistered."
            )
        } catch (e: Exception) {
            log(
                "AccessibilityBroadcastService",
                "    ~~> Unregister exception"
            )
        } finally {
            instance = null //TODO: [1] Find a better way
            super.onDestroy()
        }
    }
}