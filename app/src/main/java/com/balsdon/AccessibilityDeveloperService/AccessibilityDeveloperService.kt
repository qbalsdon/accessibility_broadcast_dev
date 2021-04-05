package com.balsdon.AccessibilityDeveloperService

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Path
import android.media.AudioManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.*

/*
flagRequestAccessibilityButton: Will show the accessibility button on the bottom right hand side
 */
class AccessibilityDeveloperService : AccessibilityService() {
    companion object {
        //TODO: [1] Not a huge fan of this...
        //https://developer.android.com/reference/android/content/BroadcastReceiver#peekService(android.content.Context,%20android.content.Intent)
        var instance: AccessibilityDeveloperService? = null

        private val accessibilityButtonCallback =
            object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    log(
                        "AccessibilityDeveloperService",
                        "    ~~> AccessibilityButtonCallback"
                    )

                    // Add custom logic for a service to react to the
                    // accessibility button being pressed.
                }

                override fun onAvailabilityChanged(
                    controller: AccessibilityButtonController,
                    available: Boolean
                ) {
                    log(
                        "AccessibilityDeveloperService",
                        "    ~~> AccessibilityButtonCallback availability [$available]"
                    )
                }
            }
    }

    private val accessibilityActionReceiver = AccessibilityActionReceiver()
    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    private val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }
    private val halfWidth: Float by lazy { (displayMetrics.widthPixels) / 2f }
    private val halfHeight: Float by lazy { (displayMetrics.heightPixels) / 2f }
    private val quarterWidth: Float by lazy { halfWidth / 2f }
    private val quarterHeight: Float by lazy { halfWidth / 2f }

    //REQUIRED overrides... not used
    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        log("AccessibilityDeveloperService", "  ~~> onAccessibilityEvent [$event]")

        if (event?.eventType == TYPE_WINDOW_CONTENT_CHANGED) {
            log("AccessibilityDeveloperService", "  ~~> onAccessibilityEvent [$event]")
        }
    }

    fun findFocusedViewInfo(): AccessibilityNodeInfo = with(rootInActiveWindow) {
        val viewInfo = this.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        log(
            "AccessibilityDeveloperService",
            "  ~~> View in focus: [${viewInfo.className} : ${viewInfo.viewIdResourceName}]"
        )
        return viewInfo
    }

    override fun onServiceConnected() {
        log(
            "AccessibilityDeveloperService",
            "onServiceConnected"
        )
        registerReceiver(accessibilityActionReceiver, IntentFilter().apply {
            addAction(ACCESSIBILITY_CONTROL_BROADCAST_ACTION)
            priority = 100
            log(
                "AccessibilityDeveloperService",
                "    ~~> Receiver is registered."
            )
        })
        instance = this

        //https://developer.android.com/guide/topics/ui/accessibility/service
        if (accessibilityButtonController.isAccessibilityButtonAvailable) {
            accessibilityButtonController.registerAccessibilityButtonCallback(
                accessibilityButtonCallback
            )
        }
    }

    // My confirmation bias allowed me to mix up "scroll" and "navigate" - this is for things that scroll
    // on screens, like actual ScrollViews, RecyclerViews etc. This is not a helper for focus
    private fun findScrollableNode(): AccessibilityNodeInfo? {
        val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(rootInActiveWindow)
        while (!deque.isEmpty()) {
            val node: AccessibilityNodeInfo = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node
            }
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    private fun navNodeBFS(node: AccessibilityNodeInfo = rootInActiveWindow) {

    }

    fun debugAction() {
        findFocusedViewInfo().traversalBefore.performAction(ACTION_CLICK)
        //log("DEBUG ACTIONS", "ACTIONS: ${}")
    }

    private fun createVerticalSwipePath(downToUp: Boolean): Path = Path().apply {
        if (downToUp) {
            moveTo(halfWidth - quarterWidth, halfHeight - quarterHeight)
            lineTo(halfWidth - quarterWidth, halfHeight + quarterHeight)
        } else {
            moveTo(halfWidth - quarterWidth, halfHeight + quarterHeight)
            lineTo(halfWidth - quarterWidth, halfHeight - quarterHeight)
        }
    }

    private fun createHorizontalSwipePath(leftToRight: Boolean): Path = Path().apply {
        if (leftToRight) {
            moveTo(halfWidth - quarterWidth, halfHeight)
            lineTo(halfWidth + quarterWidth, halfHeight)
        } else {
            moveTo(halfWidth + quarterWidth, halfHeight)
            lineTo(halfWidth - quarterWidth, halfHeight)
        }
    }

    fun swipeHorizontal(leftToRight: Boolean) =
        performGesture(GestureAction(createHorizontalSwipePath(leftToRight)))

    fun swipeVertical(downToUp: Boolean = true) =
        performGesture(GestureAction(createVerticalSwipePath(downToUp)))

    fun swipeUpThenDown() =
        performGesture(
            GestureAction(createVerticalSwipePath(true)),
            GestureAction(createVerticalSwipePath(false), 500)
        )


    fun threeFingerSwipeUp() {
        val stX = halfWidth - quarterWidth
        val stY = halfHeight + quarterHeight
        val enY = halfHeight - quarterHeight
        val eighth = quarterWidth / 2f

        val one = Path().apply {
                moveTo(stX - eighth, stY)
                lineTo(stX - eighth, enY)
            }
        val two = Path().apply {
                moveTo(stX, stY)
                lineTo(stX, enY)
            }
        val three = Path().apply {
                moveTo(stX + eighth, stY)
                lineTo(stX + eighth, enY)
            }

        performGesture(GestureAction(one), GestureAction(two), GestureAction(three))
    }

    //https://developer.android.com/guide/topics/ui/accessibility/service#continued-gestures
    //TODO: Fix this
    fun swipeUpRight() {
        val stX = halfWidth - quarterWidth
        val enX = halfWidth + quarterWidth

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

        performGesture(GestureAction(swipeUp), GestureAction(swipeLeftToRight, 500))
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

    private fun performGesture(vararg gestureActions: GestureAction) =
        dispatchGesture(createGestureFrom(*gestureActions), GestureResultCallback(baseContext), null)


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
        log("AccessibilityDeveloperService", "  ~~> Volume set to value [$volume]")
        audioManager.setStreamVolume(
            AudioManager.STREAM_ACCESSIBILITY,
            volume,
            AudioManager.FLAG_SHOW_UI
        )
    }

    override fun onDestroy() {
        log(
            "AccessibilityDeveloperService",
            "  ~~> onDestroy"
        )
        // Unregister accessibilityActionReceiver when destroyed.
        // I have had bad luck with broadcast receivers in the past
        try {
            unregisterReceiver(accessibilityActionReceiver)
            accessibilityButtonController.unregisterAccessibilityButtonCallback(
                accessibilityButtonCallback
            )
            log(
                "AccessibilityDeveloperService",
                "    ~~> Receiver is unregistered.\r\n    ~~> AccessibilityButtonCallback is unregistered."
            )
        } catch (e: Exception) {
            log(
                "AccessibilityDeveloperService",
                "    ~~> Unregister exception: [$e]"
            )
        } finally {
            instance = null //TODO: [1] Find a better way
            super.onDestroy()
        }
    }
}