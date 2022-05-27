package com.balsdon.accessibilityBroadcastService

import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RefreshServiceState(
    private val activity: AppCompatActivity,
    private val refreshIntervalMs: Long = 250
) {

    private fun getEnabledServices(): List<String> = try {
        Settings
            .Secure
            .getString(
                activity.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            .split(":")
    } catch (exception: NullPointerException) {
        exception.printStackTrace()
        emptyList()
    }

    val accessibilityServices: Flow<List<String>> = flow {
        while (true) {
            emit(getEnabledServices()) // Emits the result of the request to the flow
            delay(refreshIntervalMs) // Suspends the coroutine for some time
        }
    }
}