package com.balsdon.accessibilityDeveloperService.data

import android.view.accessibility.AccessibilityEvent

data class EventData(
    val eventText: String = "",
    val className: String = "",
    val isPassword: Boolean = false,
    val isEnabled: Boolean = false,
    val isChecked: Boolean = false,
    val isScrollable: Boolean = false,
    val isHeading: Boolean = false,
    val isEditable: Boolean = false
) {
    companion object {
        fun from(a11yEvent: AccessibilityEvent): EventData {
            var previousEventNodeIsHeading = false
            var previousEventNodeIsEditable = false
            try {
                with(a11yEvent.source) {
                    previousEventNodeIsHeading = this.isHeading
                    previousEventNodeIsEditable = this.isEditable
                    recycle()
                }
            } catch (exception: NullPointerException) {
                exception.printStackTrace()
            }

            return EventData().copy(
                eventText = a11yEvent.text.toString(),
                className = a11yEvent.className.toString(),
                isPassword = a11yEvent.isPassword,
                isEnabled = a11yEvent.isEnabled,
                isChecked = a11yEvent.isChecked,
                isScrollable = a11yEvent.isScrollable,
                isHeading = previousEventNodeIsHeading,
                isEditable = previousEventNodeIsEditable
            )
        }
    }
}