package com.balsdon.accessibilityBroadcastService

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.balsdon.accessibilityDeveloperService.R
import junit.framework.TestCase
import org.hamcrest.Matchers
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DemoActivityTest : TestCase() {
    @get:Rule
    val activityTestRule = ActivityScenarioRule(DemoActivity::class.java)

    companion object {
        @BeforeClass
        @JvmStatic
        fun enableAccessibilityChecks() {
            AccessibilityChecks
                .enable()
                .setRunChecksFromRootView(true)
        }
    }

    private fun createActivityScenarioRule(withNightMode: Boolean = false) =
        activityTestRule.scenario.apply {
            onActivity {
                AppCompatDelegate.setDefaultNightMode(
                    if (withNightMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

    private fun checkTitle() {
        onView(
            Matchers.allOf(
                Matchers.instanceOf(AppCompatTextView::class.java),
                ViewMatchers.withParent(withId(R.id.toolbar))
            )
        ).check(matches(ViewMatchers.withText("Accessibility Broadcast Dev")))
    }

    private fun clickPrev() {
        onView(withId(R.id.swipeLeft))
            .perform(click())
    }

    @Test
    fun testInNormalMode() {
        createActivityScenarioRule()
        checkTitle()
        clickPrev()
    }

    @Test
    fun testInNightMode() {
        createActivityScenarioRule(true)
        checkTitle()
        clickPrev()
    }
}