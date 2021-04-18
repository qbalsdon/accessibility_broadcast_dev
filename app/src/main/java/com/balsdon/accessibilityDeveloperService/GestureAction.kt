package com.balsdon.accessibilityDeveloperService

import android.accessibilityservice.GestureDescription
import android.graphics.Path

data class GestureAction(val path: Path, val startTime: Long = 0, val duration: Long = 500)

fun createGestureFrom(vararg gestureActions: GestureAction): GestureDescription {
    require(gestureActions.isNotEmpty()) { "Must perform at least 1 action" }
    return GestureDescription.Builder().addStroke(
        GestureDescription.StrokeDescription(
            gestureActions[0].path,
            gestureActions[0].startTime,
            gestureActions[0].duration,
            gestureActions.size > 1
        ).apply {
            for (index in 1 until gestureActions.size) {
                with(gestureActions[index]) {
                    continueStroke(
                        path,
                        startTime,
                        duration,
                        gestureActions.last() == this
                    )
                }
            }
        }
    ).build()
}