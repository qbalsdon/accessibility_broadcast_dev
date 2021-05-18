package com.balsdon.accessibilityDeveloperService

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.*
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat


/*
flagRequestAccessibilityButton: Will show the accessibility button on the bottom right hand side
 */
class AccessibilityDeveloperService : AccessibilityService() {
    enum class SelectionType {
        ELEMENT_ID, ELEMENT_TYPE, ELEMENT_TEXT, ELEMENT_HEADING
    }

    companion object {
        //TODO: BUG [03] Not a huge fan of this...
        //https://developer.android.com/reference/android/content/BroadcastReceiver#peekService(android.content.Context,%20android.content.Intent)
        var instance: AccessibilityDeveloperService? = null
        val DIRECTION_FORWARD = "DIRECTION_FORWARD"
        val DIRECTION_BACK = "DIRECTION_BACK"

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

    private fun Context.AccessibilityManager() =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val accessibilityActionReceiver = AccessibilityActionReceiver()
    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    private val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }
    private val halfWidth: Float by lazy { (displayMetrics.widthPixels) / 2f }
    private val halfHeight: Float by lazy { (displayMetrics.heightPixels) / 2f }
    private val quarterWidth: Float by lazy { halfWidth / 2f }
    private val quarterHeight: Float by lazy { halfWidth / 2f }

    private var curtainView: FrameLayout? = null

    private fun <T : View> findElement(@IdRes resId: Int): T =
        curtainView?.findViewById<T>(resId)
            ?: throw RuntimeException("Required view not found: CurtainView")

    private val announcementTextView: TextView
        get() {
            return findElement(R.id.announcementText)
        }
    private val classNameTextView: TextView
        get() {
            return findElement(R.id.className)
        }
    private val enabledCheckBox: CheckBox
        get() {
            return findElement(R.id.enabled)
        }
    private val checkedCheckBox: CheckBox
        get() {
            return findElement(R.id.checked)
        }
    private val scrollableCheckBox: CheckBox
        get() {
            return findElement(R.id.scrollable)
        }
    private val passwordCheckBox: CheckBox
        get() {
            return findElement(R.id.password)
        }
    private val headingCheckBox: CheckBox
        get() {
            return findElement(R.id.heading)
        }
    private val editableCheckBox: CheckBox
        get() {
            return findElement(R.id.editable)
        }

    //REQUIRED overrides... not used
    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        log("AccessibilityDeveloperService", "  ~~> onAccessibilityEvent [$event]")

        if (event.eventType == TYPE_WINDOW_STATE_CHANGED) return

        if (!event.text.isNullOrEmpty() && curtainView != null) {
            log("AccessibilityDeveloperService", "  ~~> Announce [$event]")
            announcementTextView.text = event.text.toString()
                .replace('[', ' ')
                .replace(']', ' ')
                .trim()

            classNameTextView.text = event.className
            passwordCheckBox.isChecked = event.isPassword
            enabledCheckBox.isChecked = event.isEnabled
            checkedCheckBox.isChecked = event.isChecked
            scrollableCheckBox.isChecked = event.isChecked

            val currentNode = this.findFocus(FOCUS_ACCESSIBILITY)
            if (currentNode == null) {
                headingCheckBox.isChecked = false
                editableCheckBox.isChecked = false
            } else {
                headingCheckBox.isChecked = currentNode.isHeading
                editableCheckBox.isChecked = currentNode.isEditable
            }
        }
    }

    fun findFocusedViewInfo(): AccessibilityNodeInfoCompat = with(rootInActiveWindow) {
        val viewInfo = this.findFocus(FOCUS_ACCESSIBILITY)
        log(
            "AccessibilityDeveloperService",
            "  ~~> View in focus: [${viewInfo.className} : ${viewInfo.viewIdResourceName}]"
        )
        return AccessibilityNodeInfoCompat.wrap(viewInfo)
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

    fun toggleCurtain() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (curtainView == null) {
            curtainView = FrameLayout(this)
            val lp = WindowManager.LayoutParams()
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            lp.format = PixelFormat.OPAQUE
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.TOP
            val inflater = LayoutInflater.from(this)
            inflater.inflate(R.layout.accessibility_curtain, curtainView)
            wm.addView(curtainView, lp)
        } else {
            wm.removeView(curtainView)
            curtainView = null
        }
    }

    private fun dfsTree(
        currentNode: AccessibilityNodeInfo = rootInActiveWindow,
        depth: Int = 0
    ): List<Pair<AccessibilityNodeInfo, Int>> {
        val list = mutableListOf(Pair(currentNode, depth))
        if (currentNode.childCount > 0) {
            for (index in 0 until currentNode.childCount) {
                list.addAll(dfsTree(currentNode.getChild(index), depth + 1))
            }
        }
        return list
    }

    fun debugAction() {
        dfsTree().forEach {
            val compatNode = AccessibilityNodeInfoCompat.wrap(it.first)
            log("dfsTree", "${it.second}->[${compatNode}]$compatNode")
        }
    }

    fun announceText(speakText: String) =
        AccessibilityManager().apply {
            sendAccessibilityEvent(AccessibilityEvent.obtain().apply {
                eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                text.add(speakText)
            })
        }

    private fun focusBy(next: Boolean? = null, comparison: (AccessibilityNodeInfo) -> Boolean) {
        val tree = if (next == false) dfsTree().asReversed() else dfsTree()
        val currentNode = this.findFocus(FOCUS_ACCESSIBILITY)
        if (currentNode == null) {
            val firstNode = tree.firstOrNull { comparison(it.first) }
            firstNode?.first?.performAction(ACTION_ACCESSIBILITY_FOCUS)
            return
        }

        val index = tree.indexOfFirst { it.first == currentNode }
        if (next == null) {
            for (currentIndex in tree.indices) {
                if (comparison(tree[currentIndex].first)) {
                    tree[currentIndex].first.performAction(ACTION_ACCESSIBILITY_FOCUS)
                    return
                }
            }
        } else {
            for (currentIndex in index + 1 until tree.size) {
                if (comparison(tree[currentIndex].first)) {
                    tree[currentIndex].first.performAction(ACTION_ACCESSIBILITY_FOCUS)
                    return
                }
            }
        }
    }

    //TODO: Bug [02]: Need to scroll to element if it's not in view
    fun focus(type: SelectionType, value: String, next: Boolean = true) {
        when (type) {
            SelectionType.ELEMENT_ID -> focusBy(null) {
                it.viewIdResourceName?.toLowerCase()?.contains(value.toLowerCase()) ?: false
            }
            SelectionType.ELEMENT_TEXT -> focusBy(null) {
                it.text?.toString()?.toLowerCase()?.contains(value.toLowerCase()) ?: false
            }
            SelectionType.ELEMENT_TYPE -> focusBy(next) { it.className == value }
            SelectionType.ELEMENT_HEADING -> focusBy(next) { it.isHeading }
        }
    }

    fun click(long: Boolean = false) {
        findFocusedViewInfo().performAction(if (long) ACTION_LONG_CLICK else ACTION_CLICK)
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

    private fun createHorizontalSwipePath(rightToLeft: Boolean): Path = Path().apply {
        if (rightToLeft) {
            moveTo(halfWidth + quarterWidth, halfHeight)
            lineTo(halfWidth - quarterWidth, halfHeight)
        } else {
            moveTo(halfWidth - quarterWidth, halfHeight)
            lineTo(halfWidth + quarterWidth, halfHeight)
        }
    }

    fun swipeHorizontal(rightToLeft: Boolean) =
        performGesture(GestureAction(createHorizontalSwipePath(rightToLeft)))

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
    //TODO: BUG [01] Menu not appearing
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
        dispatchGesture(
            createGestureFrom(*gestureActions),
            GestureResultCallback(baseContext),
            null
        )


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
            instance = null
            super.onDestroy()
        }
    }
}