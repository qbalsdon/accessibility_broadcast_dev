package com.balsdon.AccessibilityDeveloperService

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

const val ACCESSIBILITY_CONTROL_BROADCAST_ACTION = "com.balsdon.talkback.accessibility"

fun Context.showToast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT)
        .show()
}

fun log(label: String, message: String) {
    android.util.Log.d(label, "[$label]: $message")
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        setUpButtons()
    }

    override fun onResume() {
        super.onResume()
        updateVolume()
    }

    private fun updateVolume() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY)
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY)
        val percent = BigDecimal(vol.toDouble() / max.toDouble()).setScale(2, RoundingMode.HALF_EVEN) * BigDecimal(100)
        volumeLabel.text = "${percent.toInt()}"
    }

    private fun broadcastAction(actionParameter: String) = sendBroadcast(Intent().apply {
        action = ACCESSIBILITY_CONTROL_BROADCAST_ACTION
        putExtra(
            AccessibilityActionReceiver.ACCESSIBILITY_ACTION,
            actionParameter
        )
    }, null)

    private fun setUpButtons() {
        navigateLeft.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_PREV)
        }

        navigateRight.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_NEXT)
        }

        volumeUp.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_VOLUME_UP)
            delayThen(1000) { updateVolume() }
        }

        volumeDown.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_VOLUME_DOWN)
            delayThen(1000) { updateVolume() }
        }

        debugAction.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_DEBUG)
        }

        //TODO: Fix this!
        openMenu.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_MENU)
        }

        settingsButton.setOnClickListener {
            //adb shell am start -n com.android.settings/.Settings$AccessibilitySettingsActivity
            startActivity(Intent().apply {
                component =
                    ComponentName.unflattenFromString("com.android.settings/.Settings\$AccessibilitySettingsActivity")
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
        }
    }

    private fun delayThen(millis: Long, function: () -> Unit) =
        MainScope().launch {
            withContext(Dispatchers.IO) { // ensures running on separate thread
                delay(millis)
                runOnUiThread {
                    function()
                }
            }
        }

    private val navigateLeft: Button by lazy { findViewById(R.id.navigateLeft) }
    private val navigateRight: Button by lazy { findViewById(R.id.navigateRight) }
    private val openMenu: Button by lazy { findViewById(R.id.openMenu) }
    private val debugAction: Button by lazy { findViewById(R.id.debugAction) }
    private val settingsButton: Button by lazy { findViewById(R.id.settingsButton) }
    private val volumeUp: AppCompatImageButton by lazy { findViewById(R.id.volumeUp) }
    private val volumeDown: AppCompatImageButton by lazy { findViewById(R.id.volumeDown) }
    private val volumeLabel: TextView by lazy { findViewById(R.id.volumeLabel) }

    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
}