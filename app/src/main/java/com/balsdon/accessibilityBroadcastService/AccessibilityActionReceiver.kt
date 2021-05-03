package com.balsdon.accessibilityDeveloperService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.balsdon.accessibilityDeveloperService.AccessibilityDeveloperService.Companion.DIRECTION_BACK
import com.balsdon.accessibilityDeveloperService.AccessibilityDeveloperService.Companion.DIRECTION_FORWARD

class AccessibilityActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACCESSIBILITY_ACTION = "ACTION"

        const val ACTION_SWIPE_LEFT = "ACTION_SWIPE_LEFT"
        const val ACTION_SWIPE_RIGHT = "ACTION_SWIPE_RIGHT"

        const val ACTION_SWIPE_UP = "ACTION_SWIPE_UP"
        const val ACTION_SWIPE_DOWN = "ACTION_SWIPE_DOWN"
        const val ACTION_CLICK = "ACTION_CLICK"
        const val ACTION_LONG_CLICK = "ACTION_LONG_CLICK"
        const val ACTION_CURTAIN = "ACTION_CURTAIN"

        const val ACTION_FOCUS_ELEMENT = "ACTION_FOCUS_ELEMENT"

        const val ACTION_VOLUME_UP = "ACTION_VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "ACTION_VOLUME_DOWN"
        const val ACTION_VOLUME_SET = "ACTION_VOLUME_SET"
        const val ACTION_VOLUME_MUTE = "ACTION_VOLUME_MUTE"

        const val ACTION_WHICH = "ACTION_WHICH"
        const val ACTION_DEBUG = "ACTION_DEBUG"

        const val ACTION_SAY = "ACTION_SAY"

        const val PARAMETER_VOLUME = "PARAMETER_VOLUME"
        const val PARAMETER_ID = "PARAMETER_ID"
        const val PARAMETER_TEXT = "PARAMETER_TEXT"
        const val PARAMETER_HEADING = "PARAMETER_HEADING"
        const val PARAMETER_DIRECTION = "PARAMETER_DIRECTION"
        const val PARAMETER_TYPE = "PARAMETER_TYPE"
    }

    private fun showError(context: Context, message: String) {
        log("AccessibilityDeveloperService", message)
        context.showToast(message)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        require(context != null) { "Context is required" }
        require(intent != null) { "Intent is required" }
        require(AccessibilityDeveloperService.instance != null) { "Service is required" }
        val serviceReference = AccessibilityDeveloperService.instance!!

        intent.getStringExtra(ACCESSIBILITY_ACTION)?.let {
            log("AccessibilityActionReceiver", "  ~~> ACTION: [$it]")
            serviceReference.apply {
                when (it) {
                    ACTION_DEBUG -> debugAction()

                    ACTION_SWIPE_LEFT -> swipeHorizontal(true)
                    ACTION_SWIPE_RIGHT -> swipeHorizontal(false)
                    ACTION_SWIPE_UP -> swipeVertical(true)
                    ACTION_SWIPE_DOWN -> swipeVertical(false)
                    ACTION_CLICK -> click()
                    ACTION_LONG_CLICK -> click(true)
                    ACTION_CURTAIN -> toggleCurtain()
                    ACTION_SAY -> {
                        if (intent.hasExtra(PARAMETER_TEXT)) {
                            val value = intent.getStringExtra(PARAMETER_TEXT)
                            log(
                                "AccessibilityActionReceiver [$ACTION_SAY]",
                                "    ~~> TYPE: [$PARAMETER_TEXT]: $value"
                            )
                            if (value == null) {
                                showError(context, "Required value: $PARAMETER_TEXT")
                                return
                            } else {
                                announceText(value)
                            }
                        }
                    }

                    ACTION_FOCUS_ELEMENT -> {
                        if (intent.hasExtra(PARAMETER_ID)) {
                            val value = intent.getStringExtra(PARAMETER_ID)
                            log(
                                "AccessibilityActionReceiver",
                                "    ~~> TYPE: [$PARAMETER_ID]: $value"
                            )
                            if (value == null) {
                                showError(context, "Required value: $PARAMETER_ID")
                                return
                            }
                            focus(AccessibilityDeveloperService.SelectionType.ELEMENT_ID, value)
                        } else if (intent.hasExtra(PARAMETER_TEXT)) {
                            val value = intent.getStringExtra(PARAMETER_TEXT)
                            log(
                                "AccessibilityActionReceiver",
                                "    ~~> TYPE: [$PARAMETER_TEXT]: $value"
                            )
                            if (value == null) {
                                showError(context, "Required value: $PARAMETER_TEXT")
                                return
                            }
                            focus(AccessibilityDeveloperService.SelectionType.ELEMENT_TEXT, value)
                        } else if (intent.hasExtra(PARAMETER_TYPE)) {
                            if (intent.hasExtra(PARAMETER_DIRECTION)) {
                                val dir = (intent.getStringExtra(PARAMETER_DIRECTION)
                                    ?: DIRECTION_FORWARD).toUpperCase() == DIRECTION_FORWARD
                                log(
                                    "AccessibilityActionReceiver",
                                    "    ~~> TYPE: [$PARAMETER_TYPE]: $dir"
                                )
                                val value = intent.getStringExtra(PARAMETER_TYPE)
                                if (value == null) {
                                    showError(context, "Required value: $PARAMETER_TYPE")
                                    return
                                }
                                focus(
                                    AccessibilityDeveloperService.SelectionType.ELEMENT_TYPE,
                                    value,
                                    dir
                                )
                            } else {
                                showError(
                                    context,
                                    "ERROR: PARAMETER_DIRECTION REQUIRED, EITHER [$DIRECTION_FORWARD] OR [$DIRECTION_BACK]"
                                )
                            }
                        } else if (intent.hasExtra(PARAMETER_HEADING)) {
                            val dir = (intent.getStringExtra(PARAMETER_HEADING)
                                ?: DIRECTION_FORWARD).toUpperCase() == DIRECTION_FORWARD
                            log(
                                "AccessibilityActionReceiver",
                                "    ~~> TYPE: [$PARAMETER_HEADING]: DIRECTION: $dir"
                            )
                            focus(
                                AccessibilityDeveloperService.SelectionType.ELEMENT_HEADING,
                                "",
                                dir
                            )
                        } else {
                            showError(
                                context,
                                "$ACTION_FOCUS_ELEMENT requires a parameter of $PARAMETER_ID, $PARAMETER_TEXT, $PARAMETER_TYPE or $PARAMETER_HEADING"
                            )
                        }
                    }
                    ACTION_VOLUME_UP -> adjustVolume(true)
                    ACTION_VOLUME_DOWN -> adjustVolume(false)

                    ACTION_VOLUME_SET -> setVolume(intent.getIntExtra(PARAMETER_VOLUME, 10))
                    ACTION_VOLUME_MUTE -> setVolume(0)
                    ACTION_WHICH -> findFocusedViewInfo()

                    else -> with("ERROR: Unknown action") {
                        showError(context, it)
                    }
                }
            }
        } ?: serviceReference.swipeHorizontal(true)
    }
}