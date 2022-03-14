package com.balsdon.accessibilityDeveloperService

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
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
import com.balsdon.accessibilityBroadcastService.AccessibilityActionReceiver
import java.lang.ref.WeakReference
import java.util.*


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
        lateinit var instance: WeakReference<AccessibilityDeveloperService>
        const val DIRECTION_FORWARD = "DIRECTION_FORWARD"
        const val DIRECTION_BACK = "DIRECTION_BACK"
        private const val MAX_POSITION = 1000f
        private const val MIN_POSITION = 100f

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

    private var curtainView: FrameLayout? = null

    private fun <T : View> findElement(@IdRes resId: Int): T =
        curtainView?.findViewById<T>(resId)
            ?: throw RuntimeException("Required view not found: CurtainView")

    private val announcementTextView: TextView
        get() = findElement(R.id.announcementText)
    private val classNameTextView: TextView
        get() = findElement(R.id.className)
    private val enabledCheckBox: CheckBox
        get() = findElement(R.id.enabled)
    private val checkedCheckBox: CheckBox
        get() = findElement(R.id.checked)
    private val scrollableCheckBox: CheckBox
        get() = findElement(R.id.scrollable)
    private val passwordCheckBox: CheckBox
        get() = findElement(R.id.password)
    private val headingCheckBox: CheckBox
        get() = findElement(R.id.heading)
    private val editableCheckBox: CheckBox
        get() = findElement(R.id.editable)

    private val audioStream = AudioManager.STREAM_ACCESSIBILITY

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
        instance = WeakReference(this)

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
//        dfsTree().forEach {
//            val compatNode = AccessibilityNodeInfoCompat.wrap(it.first)
//            log("dfsTree", "${it.second}->[${compatNode}]$compatNode")
//        }
        swipeUpRight()
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
                it.viewIdResourceName?.lowercase()?.contains(value.lowercase()) ?: false
            }
            SelectionType.ELEMENT_TEXT -> focusBy(null) {
                it.text?.toString()?.lowercase()?.contains(value.lowercase()) ?: false
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
            moveTo(MAX_POSITION / 2, MIN_POSITION)
            lineTo(MAX_POSITION / 2, MAX_POSITION)
        } else {
            moveTo(MAX_POSITION / 2, MAX_POSITION)
            lineTo(MAX_POSITION / 2, MIN_POSITION)
        }
    }

    private fun createHorizontalSwipePath(rightToLeft: Boolean): Path = Path().apply {
        if (rightToLeft) {
            moveTo(MAX_POSITION, MAX_POSITION / 2)
            lineTo(MIN_POSITION, MAX_POSITION / 2)
        } else {
            moveTo(MIN_POSITION, MAX_POSITION / 2)
            lineTo(MAX_POSITION, MAX_POSITION / 2)
        }
    }

    fun swipeHorizontal(rightToLeft: Boolean) =
        performGesture(GestureAction(createHorizontalSwipePath(rightToLeft)))

    fun swipeVertical(downToUp: Boolean = true) =
        performGesture(GestureAction(createVerticalSwipePath(downToUp)))

//    fun swipeUpThenDown() =
//        performGesture(
//            GestureAction(createVerticalSwipePath(true)),
//            GestureAction(createVerticalSwipePath(false), 500)
//        )


//    fun threeFingerSwipeUp() {
//        val one = Path().apply {
//            moveTo(MIN_POSITION, MAX_POSITION)
//            lineTo(MIN_POSITION, MIN_POSITION)
//        }
//        val two = Path().apply {
//            moveTo(MIN_POSITION * 2, MAX_POSITION)
//            lineTo(MIN_POSITION * 2, MIN_POSITION)
//        }
//        val three = Path().apply {
//            moveTo(MIN_POSITION * 3, MAX_POSITION)
//            lineTo(MIN_POSITION * 3, MIN_POSITION)
//        }
//
//        performGesture(GestureAction(one), GestureAction(two), GestureAction(three))
//    }

    //https://developer.android.com/guide/topics/ui/accessibility/service#continued-gestures
    /**
     * Cannot call this "open a11y menu" because actions can be mapped to different
     * gestures
     */
    fun swipeUpRight() {
        val swipeUpAndRight = Path().apply {
            moveTo(MIN_POSITION, MAX_POSITION)
            lineTo(MIN_POSITION, MIN_POSITION)
            lineTo(MAX_POSITION, MIN_POSITION)
        }
        performGesture(GestureAction(swipeUpAndRight))
    }

    // https://developer.android.com/guide/topics/ui/accessibility/service#continued-gestures
    // Taken from online documentation. Seems to have left out the dispatch of the gesture.
    // Also does not seem to be an accessibility gesture, but a "regular" gesture (I'm not sure)
    // Simulates an L-shaped drag path: 200 pixels right, then 200 pixels down.
//    private fun doRightThenDownDrag() {
//        val dragRightPath = Path().apply {
//            moveTo(200f, 200f)
//            lineTo(400f, 200f)
//        }
//        val dragRightDuration = 500L // 0.5 second
//
//        // The starting point of the second path must match
//        // the ending point of the first path.
//        val dragDownPath = Path().apply {
//            moveTo(400f, 200f)
//            lineTo(400f, 400f)
//        }
//        val dragDownDuration = 500L
//        val rightThenDownDrag = StrokeDescription(
//            dragRightPath,
//            0L,
//            dragRightDuration,
//            true
//        ).apply {
//            continueStroke(dragDownPath, dragRightDuration, dragDownDuration, false)
//        }
//    }

    private fun performGesture(vararg gestureActions: GestureAction) =
        dispatchGesture(
            createGestureFrom(*gestureActions),
            GestureResultCallback(),
            null
        )


    class GestureResultCallback() :
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
        val max = audioManager.getStreamMaxVolume(audioStream)
        val volume = (max * (percent.toFloat() / 100f)).toInt()
        log("AccessibilityDeveloperService", "  ~~> Volume set to value [$volume]")
        audioManager.setStreamVolume(
            audioStream,
            volume,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // TODO: Perhaps in the future
    fun setLanguage(language: String, country: String) {
        val locale = Locale(language, country)
        Locale.setDefault(locale)
        val resources: Resources = resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
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
            super.onDestroy()
        }
    }
}