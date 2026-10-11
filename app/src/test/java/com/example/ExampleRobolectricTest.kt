package com.example

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DynamicETAEngine
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.DestinationSnapshot
import com.example.model.DestinationType
import com.example.model.MovementState
import com.example.model.TransportMode
import com.example.model.TripState
import com.example.ui.Localization
import com.example.ui.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context matches TravelWake`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TravelWake", appName)
    }

    @Test
    fun `localization returns correct translations for English, Tamil, and Sinhala`() {
        val enTitle = Localization.get(Strings.APP_NAME, "en")
        val taTitle = Localization.get(Strings.APP_NAME, "ta")
        val siTitle = Localization.get(Strings.APP_NAME, "si")

        assertEquals("TravelWake", enTitle)
        assertEquals("டிராவல்வேக்", taTitle)
        assertEquals("ට්‍රැවල්වේක්", siTitle)

        // Wake alert string test
        val enWake = Localization.get(Strings.WAKE_ME_NOW, "en")
        val taWake = Localization.get(Strings.WAKE_ME_NOW, "ta")
        val siWake = Localization.get(Strings.WAKE_ME_NOW, "si")

        assertEquals("WAKE ME NOW", enWake)
        assertEquals("இப்போதே எழுப்பு", taWake)
        assertEquals("දැන් අවදි කරන්න", siWake)
    }

    @Test
    fun `dynamic eta engine detects movement and arrival inside radius`() {
        val engine = DynamicETAEngine()
        val dest = DestinationSnapshot(
            name = "Central Station",
            latitude = 6.9271,
            longitude = 79.8612,
            type = DestinationType.RAILWAY_STATION,
            radiusMeters = 200f
        )

        val trip = ActiveTrip(
            destination = dest,
            alertMinutesBefore = 5,
            state = TripState.TRACKING
        )

        // 1. First location reading: 2km away moving at 40 km/h
        val loc1 = Location("gps").apply {
            latitude = 6.9100
            longitude = 79.8500
            speed = 40f / 3.6f
            accuracy = 15f
            time = 1000L
        }
        val calc1 = engine.processLocationUpdate(trip, loc1)
        assertTrue("Distance should be calculated", calc1.remainingDistanceMeters > 500)
        assertNotNull(calc1.remainingMinutes)

        // 2. Location reading inside destination radius (6.9271, 79.8612)
        val locArrival = Location("gps").apply {
            latitude = 6.9271
            longitude = 79.8612
            speed = 5f / 3.6f
            accuracy = 10f
            time = 2000L
        }
        val calc2 = engine.processLocationUpdate(trip, locArrival)
        assertTrue("Distance should be inside radius", calc2.remainingDistanceMeters <= 200f)
        assertEquals(1, calc2.consecutiveReadingsInsideRadius)

        // Consecutive reading inside confirms arrival
        val locArrival2 = Location("gps").apply {
            latitude = 6.9271
            longitude = 79.8612
            speed = 2f / 3.6f
            accuracy = 10f
            time = 3000L
        }
        val calc3 = engine.processLocationUpdate(trip, locArrival2)
        assertEquals(AlarmStage.ARRIVAL_CONFIRMED, calc3.recommendedAlarmStage)
    }

    @Test
    fun `wake-up alertness strings are loaded correctly`() {
        val holdEn = Localization.get(Strings.HOLD_TO_DISMISS, "en")
        val holdTa = Localization.get(Strings.HOLD_TO_DISMISS, "ta")
        val holdSi = Localization.get(Strings.HOLD_TO_DISMISS, "si")

        assertEquals("Hold to Dismiss & Verify Awake", holdEn)
        assertEquals("அழுத்திப் பிடித்து விழிப்பை உறுதிசெய்க", holdTa)
        assertEquals("අල්ලාගෙන සිට අවදි බව තහවුරු කරන්න", holdSi)
    }

    @Test
    fun `data preservation manager handles pre-migration backup gracefully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backupResult = com.example.data.local.DataPreservationManager.createPreMigrationBackup(context)
        // Fresh install / test environment without DB file returns true (no-op)
        assertTrue(backupResult)
    }

    @Test
    fun `database migration preserves active trip and entities across schema updates`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.TravelWakeDatabase.getDatabase(context)

        // Insert saved place with new v2 fields
        val place = com.example.data.local.SavedPlaceEntity(
            id = "test_home_1",
            name = "University Hostel",
            type = DestinationType.UNIVERSITY,
            latitude = 6.9000,
            longitude = 79.8500,
            address = "Faculty Road, Colombo",
            radiusMeters = 250f,
            defaultAlertMinutes = 10,
            preferredTransportMode = TransportMode.BUS,
            notes = "Quiet study area entrance",
            countryCode = "LK"
        )
        db.savedPlaceDao().insertSavedPlace(place)

        val retrievedPlaces = db.savedPlaceDao().getAllSavedPlaces()
        assertTrue(retrievedPlaces.any { it.id == "test_home_1" })
        val retrieved = retrievedPlaces.first { it.id == "test_home_1" }
        assertEquals("Quiet study area entrance", retrieved.notes)
        assertEquals("LK", retrieved.countryCode)

        // Validate integrity check
        val report = com.example.data.local.DataPreservationManager.validateDatabaseIntegrity(context, db)
        assertTrue(report.isSuccessful)
        assertTrue(report.savedPlacesCount >= 1)
        assertEquals(2, report.databaseVersion)
    }

    @Test
    fun `removeDuplicateItems purges repeated saved places and travel history`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.TravelWakeRepository(context)

        // Insert places with duplicate names
        val p1 = com.example.model.SavedPlace(
            id = "dup_1",
            name = "Colombo Fort Interchange",
            type = DestinationType.RAILWAY_STATION,
            latitude = 6.9344,
            longitude = 79.8500,
            address = "Platform 1",
            radiusMeters = 200f,
            defaultAlertMinutes = 7,
            preferredTransportMode = TransportMode.TRAIN
        )
        val p2 = com.example.model.SavedPlace(
            id = "dup_2",
            name = "Colombo Fort Interchange",
            type = DestinationType.RAILWAY_STATION,
            latitude = 6.9344,
            longitude = 79.8500,
            address = "Platform 2",
            radiusMeters = 200f,
            defaultAlertMinutes = 7,
            preferredTransportMode = TransportMode.TRAIN
        )

        repo.addSavedPlace(p1)
        repo.addSavedPlace(p2)

        val removed = repo.removeDuplicateItems()
        assertTrue("Duplicate items should be removed or prevented", removed >= 0)

        val currentPlaces = repo.savedPlaces.value
        val colomboFortCount = currentPlaces.count { it.name.equals("Colombo Fort Interchange", ignoreCase = true) }
        assertEquals("There should only be at most 1 item for Colombo Fort Interchange", 1, colomboFortCount)
    }

    @Test
    fun `searchPlaces removes duplicate search results and curated hubs`() = kotlinx.coroutines.runBlocking {
        val provider = com.example.data.provider.NominatimGeocodingProvider()
        val results = provider.searchPlaces(
            query = "Colombo",
            userLat = 6.9271,
            userLng = 79.8612,
            preferredCountryCode = "LK",
            searchWorldwide = false
        )

        // Ensure all returned place IDs and normalized names are distinct
        val distinctById = results.distinctBy { it.placeId }
        val distinctByNameAndCity = results.distinctBy { "${it.name.trim().lowercase()}_${it.city.trim().lowercase()}" }

        assertEquals(distinctById.size, results.size)
        assertEquals(distinctByNameAndCity.size, results.size)
    }

    @Test
    fun `country search returns unique country items`() {
        val results = com.example.model.CountryData.searchCountries("land")
        val uniqueCodes = results.distinctBy { it.code }
        assertEquals("All country results must be unique", uniqueCodes.size, results.size)
    }

    @Test
    fun `gemini model constants conform to user specifications`() {
        assertEquals("gemini-3.5-flash", com.example.service.ai.GeminiConfig.MODEL_FLASH)
        assertEquals("gemini-3.8-live", com.example.service.ai.GeminiConfig.MODEL_LIVE)
    }

    @Test
    fun `grounded place converts accurately to destination snapshot and destination`() {
        val place = com.example.service.ai.GroundedPlace(
            name = "Kandy Central Railway Station",
            address = "Station Rd, Kandy",
            latitude = 7.2906,
            longitude = 80.6289,
            transportMode = TransportMode.TRAIN,
            destinationType = DestinationType.RAILWAY_STATION
        )

        val destination = place.toDestinationSnapshot(radiusMeters = 300f)
        assertEquals("Kandy Central Railway Station", destination.name)
        assertEquals(7.2906, destination.latitude, 0.0001)
        assertEquals(80.6289, destination.longitude, 0.0001)
        assertEquals(300f, destination.radiusMeters, 0.01f)
        assertEquals(TransportMode.TRAIN, destination.transportMode)
    }

    @Test
    fun `transit route catalog searches routes by number and stop name`() {
        // Search by route number
        val routes138 = com.example.data.provider.TransitRouteCatalog.searchRoutes("138", "LK")
        assertTrue("Route 138 should be found", routes138.isNotEmpty())
        assertEquals("138", routes138.first().routeNumber)
        assertEquals(TransportMode.BUS, routes138.first().mode)

        // Search by intermediate stop name along the route
        val routesViaNugegoda = com.example.data.provider.TransitRouteCatalog.searchRoutes("Nugegoda", "LK")
        assertTrue("Routes stopping at Nugegoda should be found", routesViaNugegoda.isNotEmpty())
        assertTrue("Should include Route 138", routesViaNugegoda.any { it.routeNumber == "138" })

        // Check stops have valid coordinates and non-empty name
        val stops = routes138.first().majorStops
        assertTrue("Route 138 should have multiple stops", stops.size >= 5)
        stops.forEach { stop ->
            assertTrue("Stop coordinates must be valid", stop.latitude != 0.0 && stop.longitude != 0.0)
            assertTrue("Stop name cannot be blank", stop.name.isNotBlank())
        }

        // Global search finds international metro lines
        val londonTube = com.example.data.provider.TransitRouteCatalog.searchRoutes("Piccadilly", searchWorldwide = true)
        assertTrue("London Piccadilly tube line should be found", londonTube.isNotEmpty())
        assertEquals(TransportMode.METRO, londonTube.first().mode)
    }

    @Test
    fun `estimateArrivalTime computes realistic duration, distance and future arrival timestamp`() {
        val dest = DestinationSnapshot(
            name = "Galle Railway Station",
            latitude = 6.0353,
            longitude = 80.2158,
            type = DestinationType.RAILWAY_STATION,
            transportMode = TransportMode.TRAIN
        )

        // Estimate from Colombo Fort (approx 115 km)
        val userLat = 6.9344
        val userLng = 79.8503

        val estimate = com.example.service.alarm.TransportAlarmScheduler.estimateArrivalTime(
            destination = dest,
            userLat = userLat,
            userLng = userLng
        )

        assertTrue("Distance should be greater than 100km", estimate.distanceKm > 100.0)
        assertTrue("Estimated travel duration should be positive", estimate.durationMinutes > 60)
        assertTrue("Arrival timestamp must be in the future", estimate.arrivalTimeMillis > System.currentTimeMillis())
        assertTrue("Formatted arrival time should not be empty", estimate.formattedArrivalTime.isNotBlank())
    }

    @Test
    fun `setPhoneAlarmForArrival schedules alarm accurately based on transport arrival time`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dest = DestinationSnapshot(
            name = "Kandy Central Station",
            latitude = 7.2906,
            longitude = 80.6289,
            type = DestinationType.RAILWAY_STATION,
            transportMode = TransportMode.TRAIN,
            routeName = "Main Line Intercity"
        )

        val result = com.example.service.alarm.TransportAlarmScheduler.setPhoneAlarmForArrival(
            context = context,
            destination = dest,
            alertMinutesBefore = 7,
            userLat = 6.9344,
            userLng = 79.8503,
            openSystemClockApp = false
        )

        assertTrue("Alarm schedule should succeed", result.success)
        assertEquals("Kandy Central Station", result.destinationName)
        assertEquals(7, result.alertMinutesBefore)
        assertTrue("Alarm time must be before arrival time", result.alarmTimeMillis < result.arrivalTimeMillis)
        assertTrue("Alarm timestamp must be in the future", result.alarmTimeMillis >= System.currentTimeMillis())
        assertTrue("Formatted alarm time must be present", result.formattedAlarmTime.isNotBlank())
        assertTrue("Formatted arrival time must be present", result.formattedArrivalTime.isNotBlank())
    }

    @Test
    fun `CountdownTimerUtils calculateCountdown formats hours, minutes and seconds accurately`() {
        val now = 1000000000L

        // 1. Over an hour (1 hour 15 min 42 sec = 4542 seconds = 4,542,000 ms)
        val targetOverHour = now + (4542 * 1000L)
        val stateOverHour = com.example.ui.components.CountdownTimerUtils.calculateCountdown(
            targetMillis = targetOverHour,
            currentMillis = now
        )
        assertEquals(1L, stateOverHour.hours)
        assertEquals(15L, stateOverHour.minutes)
        assertEquals(42L, stateOverHour.seconds)
        assertEquals("01:15:42", stateOverHour.formatted)
        assertEquals(false, stateOverHour.isArrived)

        // 2. Under an hour (14 min 5 sec = 845 seconds = 845,000 ms)
        val targetUnderHour = now + (845 * 1000L)
        val stateUnderHour = com.example.ui.components.CountdownTimerUtils.calculateCountdown(
            targetMillis = targetUnderHour,
            currentMillis = now
        )
        assertEquals(0L, stateUnderHour.hours)
        assertEquals(14L, stateUnderHour.minutes)
        assertEquals(5L, stateUnderHour.seconds)
        assertEquals("14:05", stateUnderHour.formatted)
        assertEquals(false, stateUnderHour.isArrived)

        // 3. Arrived / Expired (0 seconds)
        val targetArrived = now - 5000L
        val stateArrived = com.example.ui.components.CountdownTimerUtils.calculateCountdown(
            targetMillis = targetArrived,
            currentMillis = now
        )
        assertEquals(0L, stateArrived.totalRemainingSeconds)
        assertEquals("00:00", stateArrived.formatted)
        assertEquals(true, stateArrived.isArrived)
    }

    @Test
    fun `CountdownTimerUtils determineApproachStage detects stages as transport approaches`() {
        // Stage 1: Far away (8000m, 25 minutes) -> IN_TRANSIT
        val stageInTransit = com.example.ui.components.CountdownTimerUtils.determineApproachStage(
            remainingDistanceMeters = 8000.0,
            remainingSeconds = 25 * 60L,
            radiusMeters = 200f,
            alertMinutesBefore = 7,
            alarmStage = AlarmStage.NO_ALARM
        )
        assertEquals(com.example.ui.components.TransportApproachStage.IN_TRANSIT, stageInTransit)

        // Stage 2: Approaching (3500m or 10 mins) -> APPROACHING
        val stageApproaching = com.example.ui.components.CountdownTimerUtils.determineApproachStage(
            remainingDistanceMeters = 3500.0,
            remainingSeconds = 10 * 60L,
            radiusMeters = 200f,
            alertMinutesBefore = 7,
            alarmStage = AlarmStage.NO_ALARM
        )
        assertEquals(com.example.ui.components.TransportApproachStage.APPROACHING, stageApproaching)

        // Stage 3: Inside wake alert threshold (e.g. 5 mins left with 7 min threshold) -> WAKE_ALERT_ZONE
        val stageWakeZone = com.example.ui.components.CountdownTimerUtils.determineApproachStage(
            remainingDistanceMeters = 1500.0,
            remainingSeconds = 5 * 60L,
            radiusMeters = 200f,
            alertMinutesBefore = 7,
            alarmStage = AlarmStage.NO_ALARM
        )
        assertEquals(com.example.ui.components.TransportApproachStage.WAKE_ALERT_ZONE, stageWakeZone)

        // Stage 4: Arriving now (inside destination radius <= 200 * 1.4 = 280m or <= 75s) -> ARRIVING_NOW
        val stageArrivingNow = com.example.ui.components.CountdownTimerUtils.determineApproachStage(
            remainingDistanceMeters = 180.0,
            remainingSeconds = 40L,
            radiusMeters = 200f,
            alertMinutesBefore = 7,
            alarmStage = AlarmStage.NO_ALARM
        )
        assertEquals(com.example.ui.components.TransportApproachStage.ARRIVING_NOW, stageArrivingNow)
    }
}
