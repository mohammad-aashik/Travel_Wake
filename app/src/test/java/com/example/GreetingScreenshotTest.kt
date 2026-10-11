package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.ReliabilityLevel
import com.example.model.UnitSystem
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun home_screen_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    activeTrip = null,
                    savedPlaces = emptyList(),
                    recentHistory = emptyList(),
                    readiness = ReliabilityLevel.READY,
                    unitSystem = UnitSystem.METRIC,
                    language = "en",
                    hasLocationPermission = true,
                    hasNotificationPermission = true,
                    isGpsEnabled = true,
                    onStartTripClick = {},
                    onOpenLiveTripClick = {},
                    onQuickStartPlace = {},
                    onSavedPlacesClick = {},
                    onHistoryClick = {},
                    onOpenSimulator = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen.png")
    }
}
