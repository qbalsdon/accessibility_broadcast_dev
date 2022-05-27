package com.balsdon.accessibilityBroadcastService

import android.content.Context

const val ACCESSIBILITY_CONTROL_BROADCAST_ACTION = "com.balsdon.talkback.accessibility"

fun Context.showToast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT)
        .show()
}

fun log(label: String, message: String) {
    android.util.Log.d(label, "[$label]: $message")
}