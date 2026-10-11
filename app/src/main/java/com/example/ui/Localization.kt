package com.example.ui

object Strings {
    const val APP_NAME = "app_name"
    const val START_TRIP = "start_trip"
    const val ACTIVE_TRIP = "active_trip"
    const val SAVED_PLACES = "saved_places"
    const val TRAVEL_HISTORY = "travel_history"
    const val TRAVEL_READINESS = "travel_readiness"
    const val DESTINATION = "destination"
    const val DYNAMIC_ETA = "dynamic_eta"
    const val REMAINING_DISTANCE = "remaining_distance"
    const val REMAINING_TIME = "remaining_time"
    const val ALERT_BEFORE = "alert_before"
    const val WAKE_ME_NOW = "wake_me_now"
    const val END_TRIP = "end_trip"
    const val PAUSE_TRIP = "pause_trip"
    const val RESUME_TRIP = "resume_trip"
    const val SNOOZE_2_MIN = "snooze_2_min"
    const val IM_AWAKE = "im_awake"
    const val SEARCH_DESTINATION = "search_destination"
    const val SELECT_RADIUS = "select_radius"
    const val TRANSPORT_MODE = "transport_mode"
    const val WAKE_UP_TIME = "wake_up_time"
    const val GPS_STATUS = "gps_status"
    const val RELIABILITY_READY = "reliability_ready"
    const val RELIABILITY_LIMITED = "reliability_limited"
    const val RELIABILITY_NOT_RELIABLE = "reliability_not_reliable"
    const val SIMULATION_MODE = "simulation_mode"
    const val TEST_ALARM = "test_alarm"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val GUEST_MODE = "guest_mode"
    const val LOGOUT = "logout"
    const val CHANGE_PASSWORD = "change_password"
    const val DELETE_ACCOUNT = "delete_account"
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val ONBOARDING_TITLE = "onboarding_title"
    const val ONBOARDING_SUBTITLE = "onboarding_subtitle"
    const val GET_STARTED = "get_started"
    const val PERMISSIONS = "permissions"
    const val LOCATION_LOST = "location_lost"
    const val DESTINATION_PASSED = "destination_passed"
    const val WRONG_DIRECTION = "wrong_direction"
    const val ABOUT_TITLE = "about_title"
    const val ABOUT_DESC = "about_desc"
    const val HOLD_TO_DISMISS = "hold_to_dismiss"
    const val VERIFY_ALERTNESS = "verify_alertness"
    const val SNOOZE_WARNING = "snooze_warning"
    const val ALERTNESS_CONFIRMED = "alertness_confirmed"
    const val REFLEX_CHALLENGE = "reflex_challenge"
    const val TAP_TARGET_ICON = "tap_target_icon"
    const val COUNTRY = "country"
    const val SELECT_COUNTRY = "select_country"
    const val SEARCH_COUNTRY = "search_country"
    const val DETECT_MY_COUNTRY = "detect_my_country"
    const val DETECTING_COUNTRY = "detecting_country"
    const val COUNTRY_DETECTED = "country_detected"
    const val CONFIRM_COUNTRY_DETECTED = "confirm_country_detected"
    const val POPULAR_COUNTRIES = "popular_countries"
    const val ALL_COUNTRIES = "all_countries"
    const val WORLDWIDE_SEARCH = "worldwide_search"
    const val SEARCHING_IN = "searching_in"
    const val TAP_FOR_WORLDWIDE = "tap_for_worldwide"
    const val SWITCH_COUNTRY = "switch_country"
    const val STEP_LANGUAGE = "step_language"
    const val STEP_COUNTRY = "step_country"
    const val STEP_ACCOUNT = "step_account"
    const val STEP_1_OF_3 = "step_1_of_3"
    const val STEP_2_OF_3 = "step_2_of_3"
    const val STEP_3_OF_3 = "step_3_of_3"
    const val CONTINUE_TO_COUNTRY = "continue_to_country"
    const val CONTINUE_TO_ACCOUNT = "continue_to_account"
    const val GUEST_INSTANT_START = "guest_instant_start"
    const val GUEST_SUBTITLE = "guest_subtitle"
    const val OR_SIGN_IN = "or_sign_in"
    const val OR_CREATE_ACCOUNT = "or_create_account"
    const val ALREADY_HAVE_ACCOUNT = "already_have_account"
    const val DONT_HAVE_ACCOUNT = "dont_have_account"
    const val SETUP_WELCOME = "setup_welcome"
    const val SETUP_WELCOME_DESC = "setup_welcome_desc"
    const val COUNTRY_PRIORITY_NOTE = "country_priority_note"
}

object Localization {

    private val en = mapOf(
        Strings.APP_NAME to "TravelWake",
        Strings.START_TRIP to "Start Trip",
        Strings.ACTIVE_TRIP to "Active Journey",
        Strings.SAVED_PLACES to "Saved Places",
        Strings.TRAVEL_HISTORY to "Travel History",
        Strings.TRAVEL_READINESS to "Travel Readiness",
        Strings.DESTINATION to "Destination",
        Strings.DYNAMIC_ETA to "Dynamic ETA",
        Strings.REMAINING_DISTANCE to "Distance Left",
        Strings.REMAINING_TIME to "Time to Destination",
        Strings.ALERT_BEFORE to "Wake Alert Setting",
        Strings.WAKE_ME_NOW to "WAKE ME NOW",
        Strings.END_TRIP to "End Journey",
        Strings.PAUSE_TRIP to "Pause Tracking",
        Strings.RESUME_TRIP to "Resume Tracking",
        Strings.SNOOZE_2_MIN to "Snooze (2 min)",
        Strings.IM_AWAKE to "I'm Awake",
        Strings.SEARCH_DESTINATION to "Search stations, stops, landmarks...",
        Strings.SELECT_RADIUS to "Arrival Geofence Radius",
        Strings.TRANSPORT_MODE to "Transport Mode",
        Strings.WAKE_UP_TIME to "Wake-up minutes before arrival",
        Strings.GPS_STATUS to "GPS Status",
        Strings.RELIABILITY_READY to "Ready — Systems operational",
        Strings.RELIABILITY_LIMITED to "Limited — Check background or battery",
        Strings.RELIABILITY_NOT_RELIABLE to "Not Reliable — Action required",
        Strings.SIMULATION_MODE to "Transit Simulator",
        Strings.TEST_ALARM to "Test Sound & Alarm",
        Strings.SETTINGS to "Settings & Privacy",
        Strings.ACCOUNT to "Passenger Account",
        Strings.LOGIN to "Log In",
        Strings.SIGN_UP to "Create Account",
        Strings.GUEST_MODE to "Continue as Guest",
        Strings.LOGOUT to "Log Out",
        Strings.CHANGE_PASSWORD to "Change Password",
        Strings.DELETE_ACCOUNT to "Delete Account",
        Strings.LANGUAGE to "App Language",
        Strings.THEME to "Theme Appearance",
        Strings.ONBOARDING_TITLE to "Sleep soundly, arrive on time",
        Strings.ONBOARDING_SUBTITLE to "TravelWake recalculates your arrival using real-time motion and transit corridors, waking you before your stop.",
        Strings.GET_STARTED to "Get Started",
        Strings.PERMISSIONS to "Permission Center",
        Strings.LOCATION_LOST to "Location Signal Lost",
        Strings.DESTINATION_PASSED to "Destination Passed!",
        Strings.WRONG_DIRECTION to "Moving away from destination",
        Strings.ABOUT_TITLE to "About TravelWake",
        Strings.ABOUT_DESC to "Global public transport sleep assistant built with dynamic arrival recalculation.",
        Strings.HOLD_TO_DISMISS to "Hold to Dismiss & Verify Awake",
        Strings.VERIFY_ALERTNESS to "Keep holding to verify alertness",
        Strings.SNOOZE_WARNING to "Snooze warning: Station is approaching soon!",
        Strings.ALERTNESS_CONFIRMED to "Alertness Verified! You are awake.",
        Strings.REFLEX_CHALLENGE to "Cognitive Reflex Check",
        Strings.TAP_TARGET_ICON to "Tap the matching transport icon to disarm",
        Strings.COUNTRY to "Country / Region",
        Strings.SELECT_COUNTRY to "Select Your Country",
        Strings.SEARCH_COUNTRY to "Search country or city...",
        Strings.DETECT_MY_COUNTRY to "Detect My Country",
        Strings.DETECTING_COUNTRY to "Detecting your location...",
        Strings.COUNTRY_DETECTED to "Country Detected",
        Strings.CONFIRM_COUNTRY_DETECTED to "Set as your active country?",
        Strings.POPULAR_COUNTRIES to "Popular Countries",
        Strings.ALL_COUNTRIES to "All Countries",
        Strings.WORLDWIDE_SEARCH to "Worldwide Search",
        Strings.SEARCHING_IN to "Searching in",
        Strings.TAP_FOR_WORLDWIDE to "Tap to search Worldwide",
        Strings.SWITCH_COUNTRY to "Switch Country",
        Strings.STEP_LANGUAGE to "Language",
        Strings.STEP_COUNTRY to "Country",
        Strings.STEP_ACCOUNT to "Account",
        Strings.STEP_1_OF_3 to "Step 1 of 3: Choose Language",
        Strings.STEP_2_OF_3 to "Step 2 of 3: Select Country",
        Strings.STEP_3_OF_3 to "Step 3 of 3: Passenger Account",
        Strings.CONTINUE_TO_COUNTRY to "Continue to Country",
        Strings.CONTINUE_TO_ACCOUNT to "Continue to Account",
        Strings.GUEST_INSTANT_START to "Continue as Guest",
        Strings.GUEST_SUBTITLE to "Start travelling immediately without signing up",
        Strings.OR_SIGN_IN to "Or Sign In to Existing Account",
        Strings.OR_CREATE_ACCOUNT to "Or Create a New Account",
        Strings.ALREADY_HAVE_ACCOUNT to "Already have an account? Log In",
        Strings.DONT_HAVE_ACCOUNT to "Don't have an account? Sign Up",
        Strings.SETUP_WELCOME to "Welcome to TravelWake",
        Strings.SETUP_WELCOME_DESC to "Your smart wake-up companion for trains, buses, and public transport.",
        Strings.COUNTRY_PRIORITY_NOTE to "Prioritizes local stations and stops. You can change this anytime."
    )

    private val ta = mapOf(
        Strings.APP_NAME to "டிராவல்வேக்",
        Strings.START_TRIP to "பயணத்தை தொடங்கு",
        Strings.ACTIVE_TRIP to "நடப்பு பயணம்",
        Strings.SAVED_PLACES to "சேமிக்கப்பட்ட இடங்கள்",
        Strings.TRAVEL_HISTORY to "பயண வரலாறு",
        Strings.TRAVEL_READINESS to "பயண தயார்நிலை",
        Strings.DESTINATION to "சேருமிடம்",
        Strings.DYNAMIC_ETA to "நிகழ்நேர வருகை நேரம்",
        Strings.REMAINING_DISTANCE to "மீதமுள்ள தூரம்",
        Strings.REMAINING_TIME to "சேருமிடத்திற்கான நேரம்",
        Strings.ALERT_BEFORE to "எழுப்பும் எச்சரிக்கை நேரம்",
        Strings.WAKE_ME_NOW to "இப்போதே எழுப்பு",
        Strings.END_TRIP to "பயணத்தை முடி",
        Strings.PAUSE_TRIP to "கண்காணிப்பை நிறுத்து",
        Strings.RESUME_TRIP to "மீண்டும் தொடங்கு",
        Strings.SNOOZE_2_MIN to "2 நிமிடம் தள்ளிப்போடு",
        Strings.IM_AWAKE to "நான் விழித்துவிட்டேன்",
        Strings.SEARCH_DESTINATION to "நிலையம், நிறுத்தம், இடம் தேடுங்கள்...",
        Strings.SELECT_RADIUS to "வருகை எல்லை ஆரம்",
        Strings.TRANSPORT_MODE to "போக்குவரத்து முறை",
        Strings.WAKE_UP_TIME to "வருகைக்கு முன் எத்தனை நிமிடம்?",
        Strings.GPS_STATUS to "ஜிபிஎஸ் நிலை",
        Strings.RELIABILITY_READY to "தயார் — அனைத்தும் சரியாக இயங்குகிறது",
        Strings.RELIABILITY_LIMITED to "வரம்புக்குட்பட்டது — பின்னணி அனுமதியை சரிபார்க்கவும்",
        Strings.RELIABILITY_NOT_RELIABLE to "நம்பகத்தன்மை குறைவு — கவனம் தேவை",
        Strings.SIMULATION_MODE to "பயண சோதனை உருவகப்படுத்துதல்",
        Strings.TEST_ALARM to "அலாரம் ஒலி சோதனை",
        Strings.SETTINGS to "அமைப்புகள் & தனியுரிமை",
        Strings.ACCOUNT to "பயணியர் கணக்கு",
        Strings.LOGIN to "உள்நுழை",
        Strings.SIGN_UP to "பதிவு செய்க",
        Strings.GUEST_MODE to "விருந்தினராக தொடரவும்",
        Strings.LOGOUT to "வெளியேறு",
        Strings.CHANGE_PASSWORD to "கடவுச்சொல் மாற்று",
        Strings.DELETE_ACCOUNT to "கணக்கை நீக்கு",
        Strings.LANGUAGE to "மொழி தேர்வு",
        Strings.THEME to "தோற்றம் & தீம்",
        Strings.ONBOARDING_TITLE to "அமைதியாக உறங்குங்கள், குறித்த நேரத்தில் விழித்தெழுங்கள்",
        Strings.ONBOARDING_SUBTITLE to "டிராவல்வேக் உங்கள் வருகை நேரத்தை தொடர்ந்து கணக்கிட்டு நீங்கள் இறங்கும் முன் எழுப்புகிறது.",
        Strings.GET_STARTED to "தொடங்குங்கள்",
        Strings.PERMISSIONS to "அனுமதிகள் மையம்",
        Strings.LOCATION_LOST to "ஜிபிஎஸ் சமிக்ஞை துண்டிக்கப்பட்டது",
        Strings.DESTINATION_PASSED to "சேருமிடம் கடந்துவிட்டது!",
        Strings.WRONG_DIRECTION to "தவறான திசையில் செல்கிறீர்கள்",
        Strings.ABOUT_TITLE to "டிராவல்வேக் பற்றி",
        Strings.ABOUT_DESC to "உலகளாவிய பொதுப் போக்குவரத்து உறக்க & விழிப்பு வழிகாட்டி.",
        Strings.HOLD_TO_DISMISS to "அழுத்திப் பிடித்து விழிப்பை உறுதிசெய்க",
        Strings.VERIFY_ALERTNESS to "முழுமையாக விழித்தெழும் வரை அழுத்திப் பிடிக்கவும்",
        Strings.SNOOZE_WARNING to "எச்சரிக்கை: நிறுத்தம் மிக அருகில் உள்ளது!",
        Strings.ALERTNESS_CONFIRMED to "விழிப்பு உறுதிசெய்யப்பட்டது!",
        Strings.REFLEX_CHALLENGE to "அறிவுக்கூர்மை சரிபார்ப்பு",
        Strings.TAP_TARGET_ICON to "அலாரத்தை நிறுத்த சரியான குறியீட்டைத் தொடவும்",
        Strings.COUNTRY to "நாடு / பிராந்தியம்",
        Strings.SELECT_COUNTRY to "உங்கள் நாட்டைத் தேர்ந்தெடுக்கவும்",
        Strings.SEARCH_COUNTRY to "நாடு அல்லது நகரம் தேடவும்...",
        Strings.DETECT_MY_COUNTRY to "எனது நாட்டை கண்டறி",
        Strings.DETECTING_COUNTRY to "இருப்பிடம் கண்டறியப்படுகிறது...",
        Strings.COUNTRY_DETECTED to "நாடு கண்டறியப்பட்டது",
        Strings.CONFIRM_COUNTRY_DETECTED to "முதன்மை நாடாக அமைக்கவா?",
        Strings.POPULAR_COUNTRIES to "முக்கிய நாடுகள்",
        Strings.ALL_COUNTRIES to "அனைத்து நாடுகள்",
        Strings.WORLDWIDE_SEARCH to "உலகளாவிய தேடல்",
        Strings.SEARCHING_IN to "தேடும் நாடு",
        Strings.TAP_FOR_WORLDWIDE to "உலகளாவிய தேடலுக்கு மாற்றவும்",
        Strings.SWITCH_COUNTRY to "நாட்டை மாற்று",
        Strings.STEP_LANGUAGE to "மொழி",
        Strings.STEP_COUNTRY to "நாடு",
        Strings.STEP_ACCOUNT to "கணக்கு",
        Strings.STEP_1_OF_3 to "படி 1/3: மொழி தேர்வு",
        Strings.STEP_2_OF_3 to "படி 2/3: நாடு தேர்வு",
        Strings.STEP_3_OF_3 to "படி 3/3: பயணியர் கணக்கு",
        Strings.CONTINUE_TO_COUNTRY to "நாடு தேர்வுக்கு செல்",
        Strings.CONTINUE_TO_ACCOUNT to "கணக்கு தேர்வுக்கு செல்",
        Strings.GUEST_INSTANT_START to "விருந்தினராக தொடரவும்",
        Strings.GUEST_SUBTITLE to "பதிவு செய்யாமல் உடனடியாக பயணிக்கலாம்",
        Strings.OR_SIGN_IN to "அல்லது கணக்கில் உள்நுழையவும்",
        Strings.OR_CREATE_ACCOUNT to "அல்லது புதிய கணக்கு தொடங்கவும்",
        Strings.ALREADY_HAVE_ACCOUNT to "ஏற்கனவே கணக்கு உள்ளதா? உள்நுழையவும்",
        Strings.DONT_HAVE_ACCOUNT to "கணக்கு இல்லையா? பதிவு செய்க",
        Strings.SETUP_WELCOME to "டிராவல்வேக் வரவேற்கிறது",
        Strings.SETUP_WELCOME_DESC to "ரயில், பேருந்து பயணங்களுக்கான சிறந்த விழிப்பு வழிகாட்டி.",
        Strings.COUNTRY_PRIORITY_NOTE to "உள்ளூர் நிலையங்களுக்கு முன்னுரிமை அளிக்கிறது. எப்போது வேண்டுமானாலும் மாற்றலாம்."
    )

    private val si = mapOf(
        Strings.APP_NAME to "ට්‍රැවල්වේක්",
        Strings.START_TRIP to "ගමන ආරම්භ කරන්න",
        Strings.ACTIVE_TRIP to "ක්‍රියාකාරී ගමන",
        Strings.SAVED_PLACES to "සුරැකි ස්ථාන",
        Strings.TRAVEL_HISTORY to "ගමන් ඉතිහාසය",
        Strings.TRAVEL_READINESS to "ගමන් සූදානම",
        Strings.DESTINATION to "ගමනාන්තය",
        Strings.DYNAMIC_ETA to "තත්‍ය කාලීන පැමිණීමේ වේලාව",
        Strings.REMAINING_DISTANCE to "ඉතිරි දුර",
        Strings.REMAINING_TIME to "ගමනාන්තයට ඉතිරි කාලය",
        Strings.ALERT_BEFORE to "අවදි කිරීමේ සැකසුම",
        Strings.WAKE_ME_NOW to "දැන් අවදි කරන්න",
        Strings.END_TRIP to "ගමන අවසන් කරන්න",
        Strings.PAUSE_TRIP to "නතර කරන්න",
        Strings.RESUME_TRIP to "යළි අරඹන්න",
        Strings.SNOOZE_2_MIN to "මිනිත්තු 2ක් කල් දමන්න",
        Strings.IM_AWAKE to "මම අවදි වුණා",
        Strings.SEARCH_DESTINATION to "නැවතුම්පොළ, දුම්රියපොළ සොයන්න...",
        Strings.SELECT_RADIUS to "පැමිණීමේ සීමා අරය",
        Strings.TRANSPORT_MODE to "ප්‍රවාහන ක්‍රමය",
        Strings.WAKE_UP_TIME to "පැමිණීමට මිනිත්තු කීයකට පෙරද?",
        Strings.GPS_STATUS to "GPS තත්ත්වය",
        Strings.RELIABILITY_READY to "සූදානම් — සියලු පද්ධති ක්‍රියාකාරීයි",
        Strings.RELIABILITY_LIMITED to "සීමිතයි — පසුබිම් අවසර පරීක්ෂා කරන්න",
        Strings.RELIABILITY_NOT_RELIABLE to "විශ්වාස කළ නොහැක — අවධානය අවශ්‍යයි",
        Strings.SIMULATION_MODE to "ගමන් අනුකරණය (Simulator)",
        Strings.TEST_ALARM to "ශබ්ද පරීක්ෂාව",
        Strings.SETTINGS to "සැකසුම් සහ පෞද්ගලිකත්වය",
        Strings.ACCOUNT to "මගී ගිණුම",
        Strings.LOGIN to "ඇතුල් වන්න",
        Strings.SIGN_UP to "ගිණුමක් සාදන්න",
        Strings.GUEST_MODE to "අමුත්තෙකු ලෙස ඉදිරියට යන්න",
        Strings.LOGOUT to "ඉවත් වන්න",
        Strings.CHANGE_PASSWORD to "මුරපදය වෙනස් කරන්න",
        Strings.DELETE_ACCOUNT to "ගිණුම මකන්න",
        Strings.LANGUAGE to "භාෂාව",
        Strings.THEME to "තේමාව",
        Strings.ONBOARDING_TITLE to "සුවසේ නිදාගන්න, නියමිත නැවතුමේදී අවදි වන්න",
        Strings.ONBOARDING_SUBTITLE to "ට්‍රැවල්වේක් තත්‍ය කාලීනව ඔබගේ පැමිණීමේ වේලාව ගණනය කර බසයෙන් හෝ දුම්රියෙන් බසින්න පෙර ඔබව අවදි කරයි.",
        Strings.GET_STARTED to "ආරම්භ කරමු",
        Strings.PERMISSIONS to "අවසර මධ්‍යස්ථානය",
        Strings.LOCATION_LOST to "GPS සංඥා ඇණහිට ඇත",
        Strings.DESTINATION_PASSED to "ගමනාන්තය පසු වී ඇත!",
        Strings.WRONG_DIRECTION to "ගමනාන්තයෙන් ඉවතට ගමන් කරයි",
        Strings.ABOUT_TITLE to "ට්‍රැවල්වේක් පිළිබඳව",
        Strings.ABOUT_DESC to "ගෝලීය පොදු ප්‍රවාහන මගීන් සඳහා වන ස්වයංක්‍රීය අවදි කිරීමේ යෙදුම.",
        Strings.HOLD_TO_DISMISS to "අල්ලාගෙන සිට අවදි බව තහවුරු කරන්න",
        Strings.VERIFY_ALERTNESS to "සම්පූර්ණයෙන්ම අවදි වන තුරු අල්ලාගෙන සිටින්න",
        Strings.SNOOZE_WARNING to "අවවාදයයි: නැවතුම ආසන්නයේ ඇත!",
        Strings.ALERTNESS_CONFIRMED to "අවදි බව තහවුරු විය!",
        Strings.REFLEX_CHALLENGE to "මනස පරීක්ෂාව",
        Strings.TAP_TARGET_ICON to "අනතුරු ඇඟවීම නැවැත්වීමට නිවැරදි සලකුණ තෝරන්න",
        Strings.COUNTRY to "රට / කලාපය",
        Strings.SELECT_COUNTRY to "ඔබේ රට තෝරන්න",
        Strings.SEARCH_COUNTRY to "රට හෝ නගරය සොයන්න...",
        Strings.DETECT_MY_COUNTRY to "මගේ රට ස්වයංක්‍රීයව හඳුනාගන්න",
        Strings.DETECTING_COUNTRY to "ස්ථානය පරීක්ෂා කරමින්...",
        Strings.COUNTRY_DETECTED to "රට හඳුනාගන්නා ලදී",
        Strings.CONFIRM_COUNTRY_DETECTED to "ප්‍රධාන රට ලෙස සකසන්නද?",
        Strings.POPULAR_COUNTRIES to "ප්‍රධාන රටවල්",
        Strings.ALL_COUNTRIES to "සියලු රටවල්",
        Strings.WORLDWIDE_SEARCH to "ලෝක ව්‍යාප්ත සෙවීම",
        Strings.SEARCHING_IN to "සොයන රට",
        Strings.TAP_FOR_WORLDWIDE to "ලොව පුරා සෙවීමට මාරු වන්න",
        Strings.SWITCH_COUNTRY to "රට මාරු කරන්න",
        Strings.STEP_LANGUAGE to "භාෂාව",
        Strings.STEP_COUNTRY to "රට",
        Strings.STEP_ACCOUNT to "ගිණුම",
        Strings.STEP_1_OF_3 to "පියවර 1/3: භාෂාව තෝරන්න",
        Strings.STEP_2_OF_3 to "පියවර 2/3: රට තෝරන්න",
        Strings.STEP_3_OF_3 to "පියවර 3/3: මගී ගිණුම",
        Strings.CONTINUE_TO_COUNTRY to "රට තේරීමට යන්න",
        Strings.CONTINUE_TO_ACCOUNT to "ගිණුම් පියවරට යන්න",
        Strings.GUEST_INSTANT_START to "අමුත්තෙකු ලෙස ඉදිරියට යන්න",
        Strings.GUEST_SUBTITLE to "ලියාපදිංචි නොවී ක්ෂණිකව ගමන ආරම්භ කරන්න",
        Strings.OR_SIGN_IN to "නැතහොත් ගිණුමට ඇතුල් වන්න",
        Strings.OR_CREATE_ACCOUNT to "නැතහොත් නව ගිණුමක් සාදන්න",
        Strings.ALREADY_HAVE_ACCOUNT to "දැනටමත් ගිණුමක් තිබේද? ඇතුල් වන්න",
        Strings.DONT_HAVE_ACCOUNT to "ගිණුමක් නැද්ද? ලියාපදිංචි වන්න",
        Strings.SETUP_WELCOME to "ට්‍රැවල්වේක් වෙත සාදරයෙන් පිළිගනිමු",
        Strings.SETUP_WELCOME_DESC to "බස් සහ දුම්රිය ගමන් සඳහා ඔබේ බුද්ධිමත් අවදි කිරීමේ සහයකයා.",
        Strings.COUNTRY_PRIORITY_NOTE to "දේශීය නැවතුම්පොළ සඳහා ප්‍රමුඛතාවය ලබාදෙයි. ඕනෑම මොහොතක වෙනස් කළ හැක."
    )

    fun get(key: String, lang: String): String {
        val map = when (lang) {
            "ta" -> ta
            "si" -> si
            else -> en
        }
        return map[key] ?: en[key] ?: key
    }
}
