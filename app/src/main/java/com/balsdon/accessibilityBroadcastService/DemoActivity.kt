package com.balsdon.accessibilityBroadcastService

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import com.balsdon.accessibilityDeveloperService.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        setUpButtons()
    }

    private fun broadcastAction(actionParameter: String) = sendBroadcast(Intent().apply {
        action = ACCESSIBILITY_CONTROL_BROADCAST_ACTION
        putExtra(
            AccessibilityActionReceiver.ACCESSIBILITY_ACTION,
            actionParameter
        )
    }, null)

    private fun setUpButtons() {
        swipeRight.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_SWIPE_RIGHT)
        }

        swipeLeft.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_SWIPE_LEFT)
        }

        volumeUp.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_VOLUME_UP)
        }

        volumeDown.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_VOLUME_DOWN)
        }

        openMenu.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_SWIPE_UP_RIGHT)
        }

        devCurtainButton.setOnClickListener {
            broadcastAction(AccessibilityActionReceiver.ACTION_CURTAIN)
        }

        settingsButton.setOnClickListener {
            //adb shell am start -n com.android.settings/.Settings$AccessibilitySettingsActivity
            startActivity(Intent().apply {
                component =
                    ComponentName.unflattenFromString("com.google.android.marvin.talkback/com.android.talkback.TalkBackPreferencesActivity")
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
        }

        a11ySettingsButton.setOnClickListener {
            startActivity(Intent().apply {
                component =
                    ComponentName.unflattenFromString("com.android.settings/.Settings\$AccessibilitySettingsActivity")
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
        }
    }

    override fun onResume() {
        super.onResume()

        refreshJob = MainScope().launch {
            withContext(Dispatchers.IO) {
                RefreshServiceState(this@DemoActivity).accessibilityServices.collect { serviceList ->
                    runOnUiThread {
                        updateSwitches(serviceList)
                    }
                }
            }
        }
    }

    override fun onPause() {
        refreshJob?.cancel()
        super.onPause()
    }

    private fun updateSwitches(serviceList: List<String>) {
        talkBackSwitch.apply {
            isChecked = TALKBACK_PACKAGE_NAMES.intersect(serviceList.toSet()).isNotEmpty()
        }

        abdSwitch.apply {
            isChecked = serviceList.contains(ABD_PACKAGE_NAME)
        }
    }

    private val swipeRight: Button by lazy { findViewById(R.id.swipeRight) }
    private val swipeLeft: Button by lazy { findViewById(R.id.swipeLeft) }
    private val openMenu: Button by lazy { findViewById(R.id.openMenu) }
    private val settingsButton: Button by lazy { findViewById(R.id.settingsButton) }
    private val volumeUp: AppCompatImageButton by lazy { findViewById(R.id.volumeUp) }
    private val volumeDown: AppCompatImageButton by lazy { findViewById(R.id.volumeDown) }
    private val devCurtainButton: Button by lazy { findViewById(R.id.devCurtain) }
    private val a11ySettingsButton: Button by lazy { findViewById(R.id.a11ySettings) }

    private val talkBackSwitch: SwitchCompat by lazy { findViewById(R.id.talkbackSwitch) }
    private val abdSwitch: SwitchCompat by lazy { findViewById(R.id.abdSwitch) }

    private var refreshJob: Job? = null
}
