<div align="center">
  <img width="1200" height="475" alt="TravelWake Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />

  <h1>🌍 TravelWake</h1>

  <p><strong>Travel Freely. Rest Easy. Arrive Confidently.</strong></p>

  <p>
    An AI-assisted travel companion designed to help passengers rest during public transport journeys without constantly worrying about missing their destination.
  </p>

  <p>
    <a href="https://ai.studio/apps/aa56402a-3db0-48a0-a369-044149c03a05">View in Google AI Studio</a>
    ·
    <a href="https://github.com/nexusFour2026/Travel_Wake">GitHub Repository</a>
  </p>
</div>

---

## 📖 About TravelWake

TravelWake is a smart travel assistant designed to make public transportation journeys more comfortable and convenient.

Passengers often want to sleep, relax, or focus on other activities while travelling, but they may worry about missing their bus stop, railway station, or destination.

TravelWake aims to address this problem through destination-based monitoring, location-aware alerts, intelligent destination search, and personalized travel settings.

The long-term vision is to make TravelWake a reliable, multilingual travel companion for commuters and travellers worldwide.

## 🎯 The Problem

Public transport passengers frequently experience:

* Fear of missing their destination while sleeping.
* The need to repeatedly check their current location.
* Difficulty identifying unfamiliar stops and stations.
* Challenges when travelling in unfamiliar cities or countries.
* Inconvenience when relying only on time-based alarms.
* Difficulty accessing travel information in their preferred language.

## 💡 Our Solution

TravelWake aims to provide a destination-aware travel experience that helps users monitor their journeys and receive alerts when approaching their selected destinations.

The application is designed around five principles:

* **Convenience:** Reduce the need for constant manual journey monitoring.
* **Personalization:** Adapt the experience to the user's language and country preferences.
* **Global accessibility:** Support international destination searches.
* **Reliability:** Use location services and conventional application logic for trip monitoring.
* **Transparency:** Clearly communicate limitations when location or transport data is unavailable.

---

## ✨ Key Features

### 📍 Destination-Based Alerts

* Select a destination for a journey.
* Monitor progress using available location services.
* Configure destination-alert settings.
* Receive alerts when configured destination conditions are met.
* Preserve active-trip destination information.

Actual behaviour depends on the implemented location-tracking and notification systems, device permissions, and Android background-execution restrictions.

### 🗺️ Intelligent Destination Search

TravelWake is designed to support searches for locations such as:

* Cities and districts
* Bus stops and bus stations
* Railway stations
* Airports
* Universities and workplaces
* Tourist attractions and landmarks
* Roads and other points of interest
* Saved and recently visited places

Search availability depends on the location, geocoding, and map providers integrated into the application.

### 🌐 Multilingual Interface

The planned initial language options are:

* 🇬🇧 English
* 🇱🇰 Tamil
* 🇱🇰 Sinhala

The selected language should apply consistently across application-controlled text, including navigation, buttons, settings, notifications, dialogs, and error messages.

Additional languages can be introduced as the application evolves.

Personal notes, names, and saved-place labels should not be automatically translated without the user's permission.

### 🌍 Country-Aware Personalization

Users should be able to select their primary travel country to improve destination-search relevance.

For example, selecting Sri Lanka should prioritize relevant Sri Lankan locations when appropriate.

Country preference must not restrict international searches or override the user's actual GPS location.

### 🚆 International Travel Support

TravelWake aims to support users travelling between countries.

The application should:

* Use GPS to determine the actual location when available.
* Allow users to change their primary country.
* Keep worldwide destination search available.
* Preserve previous travel history.
* Avoid changing active-trip destinations when preferences change.
* Clearly communicate the availability and limitations of transport data.

### 👤 Account and Guest Access

The planned first-launch experience offers:

* Create an account
* Log in
* Continue as a guest

Guest users should be able to access supported core travel features without creating an account.

Users should be able to configure their language and country before entering the main application.

### 💾 Saved Places and Travel History

The intended experience includes persistent travel information, such as:

* Saved destinations
* Previous trips
* Language preferences
* Country preferences
* Notification settings
* Alarm settings
* Appearance preferences

The exact capabilities depend on the application's implemented storage and account architecture.

### 📱 Android-Friendly Interface

TravelWake aims to provide a responsive interface with:

* Proper Android status-bar safe-area handling
* Support for light and dark themes
* Adaptation to different screen sizes
* Clear navigation and settings
* Appropriate location and notification permission explanations

The Android system status bar should remain visible, and the application header should respect system window insets.

---

## 🚀 First-Launch Experience

The planned onboarding flow is:

**Open TravelWake**

↓

**Select Language**

↓

**Select Country**

↓

**Create Account / Log In / Continue as Guest**

↓

**Grant Required Permissions**

↓

**TravelWake Home**

Once setup is completed, the application should remember the user's selections instead of repeatedly asking the same questions.

Users should also be able to change their language and country later through Settings.

---

## 📡 GPS and Trip Monitoring

TravelWake separates actual geographical location from the user's preferred country.

| Component               | Purpose                                                 |
| ----------------------- | ------------------------------------------------------- |
| GPS / Location Services | Determine the user's actual location when available     |
| Country Preference      | Improve destination-search relevance                    |
| Destination Search      | Find locations using supported search providers         |
| Trip Management         | Maintain the selected destination and active-trip state |
| Distance Calculation    | Estimate the distance to the selected destination       |
| Alert Management        | Evaluate configured arrival conditions                  |

### Active Trip Protection

An active trip should preserve its relevant information, including:

* Trip ID
* Destination name
* Destination coordinates
* Destination country
* Destination type
* Route information, when available
* Transport mode
* Alert radius and settings

Changing the default country should not unexpectedly reset or replace an active trip.

---

## 🛠️ Technology and Tools

The development workflow uses Google AI Studio and Android Studio.

| Tool                      | Purpose                                                     |
| ------------------------- | ----------------------------------------------------------- |
| Google AI Studio          | AI-assisted application development and project prototyping |
| Android Studio            | Android project development and testing                     |
| Gemini API                | AI capabilities when configured and integrated              |
| Location Services         | GPS-based location information, where implemented           |
| Map / Geocoding Providers | Destination lookup, depending on the configured provider    |
| GitHub                    | Source-code version control and project collaboration       |

**Important:** The table describes the intended development environment and architecture. The exact libraries, frameworks, APIs, and services should be confirmed against the current project files.

---

## 💻 Run and Deploy Your AI Studio App

This repository contains the TravelWake project. Follow the instructions below to open and run the project locally.

### Prerequisites

* [Android Studio](https://developer.android.com/studio)
* A compatible Android SDK and build environment
* A compatible Android emulator or physical Android device
* A Gemini API key if the project requires Gemini API access
* Git, if cloning the repository through the command line

### Step 1 — Clone the Repository

Open a terminal and run:

```bash
git clone https://github.com/nexusFour2026/Travel_Wake.git
```

Navigate into the project directory:

```bash
cd Travel_Wake
```

### Step 2 — Open the Project

1. Open Android Studio.
2. Select **Open**.
3. Choose the cloned `Travel_Wake` directory.
4. Allow Gradle synchronization to complete.
5. Install any required SDK components if prompted.

If the repository uses a different project structure or build system, follow the instructions appropriate to that structure.

### Step 3 — Configure the Gemini API Key

First, inspect the project's `.env.example` file and existing API-key configuration.

If the project expects a `.env` file, create one in the appropriate project directory and add:

```dotenv
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Replace `YOUR_GEMINI_API_KEY` with your own valid API key.

**Important:**

* Do not commit your `.env` file or API key to GitHub.
* Add sensitive local configuration files to `.gitignore`.
* Confirm that the project's build configuration actually reads `.env` files.
* Android applications cannot automatically read a `.env` file merely because it exists.
* Do not expose a privileged API key inside a publicly distributed application. Use an appropriate backend or other supported secure architecture when necessary.

If the existing project uses a different secret-management mechanism, follow its existing configuration instead of introducing an incompatible one.

### Step 4 — Check the Build Configuration

Inspect the app's `build.gradle.kts` file.

The original AI Studio instructions may include a debug signing configuration similar to:

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

Remove that line **only if it is incompatible with your current project or is explicitly required by the original build instructions**.

Do not remove unrelated signing configuration or modify working build settings unnecessarily.

For release builds, configure the appropriate signing credentials securely.

### Step 5 — Run the Application

1. Connect an Android device or start an emulator.
2. Enable USB debugging if required for your physical device.
3. Select the target device in Android Studio.
4. Click **Run**.
5. Grant the permissions required by the implemented features.

Verify that the application opens and that the available functionality works as expected.

### Step 6 — Open the Google AI Studio Version

You can also access the original AI Studio project here:

**[Open TravelWake in Google AI Studio](https://ai.studio/apps/aa56402a-3db0-48a0-a369-044149c03a05)**

AI Studio project access and publishing capabilities depend on the project's sharing settings and the account being used.

### Step 7 — Google Play Upload-Key Note

If the application has already been published through Google AI Studio and you need to reset its upload key, follow the official Google Play Console guidance:

[Request an upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756)

Do this only when necessary and follow the instructions applicable to your app's Play Console configuration.

---

## 🔐 Privacy and Security

TravelWake should handle location information and account data responsibly.

Key principles include:

* Request permissions only when necessary.
* Explain the purpose of location and background permissions.
* Protect authentication credentials.
* Avoid committing API keys or other secrets to source control.
* Minimize unnecessary collection of personal information.
* Respect user permission choices.
* Preserve existing user data when changing preferences.
* Clearly explain the application's data-handling practices.

The actual privacy policy must reflect the application's implementation and any third-party services it uses.

---

## 🤖 AI Usage

AI tools have supported the planning, refinement, and documentation of TravelWake.

### ChatGPT

ChatGPT was used to help:

* Refine the problem statement and proposed solution.
* Organize feature requirements and technical documentation.
* Explore target users and competitor-analysis structure.
* Plan onboarding, language selection, and country personalization.
* Describe the intended GPS and destination-monitoring architecture.
* Improve project documentation and presentation materials.
* Prepare the AI Usage Report.

### Google AI Studio and Gemini

Google AI Studio was used as part of the AI-assisted application development workflow.

Gemini API capabilities may be used where they are configured and integrated into the application.

The exact AI functionality depends on the current implementation.

### Responsible AI Principles

AI-generated suggestions are treated as assistance rather than automatically accepted as correct.

GPS tracking, distance calculations, trip-state management, and destination alerts should rely on suitable location services and deterministic application logic.

The application should not fabricate live transport schedules, GPS coordinates, or unsupported travel information.

The project team remains responsible for implementation, testing, verification, and final decisions.

---

## 🧪 Testing and Verification

Recommended testing areas include:

* Application startup and onboarding
* Language selection and persistence
* Country selection and search relevance
* Account creation and guest access
* GPS permission handling
* Destination search
* International search
* Active-trip preservation
* Notification and alarm behaviour
* Background location behaviour
* Saved places and travel history
* API configuration and error handling
* Android status-bar safe-area layout
* Light and dark themes
* Behaviour when location or transport data is unavailable

Features should be described as implemented only after their behaviour has been verified.

---

## 🗺️ Development Roadmap

The following items represent development goals. Their completion status should be checked against the current source code.

* [ ] Complete first-launch personalization.
* [ ] Implement consistent multilingual support.
* [ ] Implement persistent country preferences.
* [ ] Improve country-aware destination search.
* [ ] Maintain worldwide search availability.
* [ ] Verify GPS-based destination monitoring.
* [ ] Verify notifications and background execution.
* [ ] Preserve active trips when preferences change.
* [ ] Complete Android status-bar safe-area corrections.
* [ ] Verify saved places and travel-history persistence.
* [ ] Review account security and data handling.
* [ ] Test across supported Android devices.
* [ ] Document supported map and transport data providers.

---

## ⚠️ Limitations

TravelWake's capabilities depend on the implemented features, device hardware, Android permissions, and external data providers.

Potential limitations include:

* GPS inaccuracies in certain environments.
* Android background-execution restrictions.
* Unavailable live public transport data.
* Differences in map and geocoding coverage.
* Network connectivity requirements.
* External API availability and usage limits.

The application should clearly distinguish verified information from estimates and unavailable data.

---

## 🤝 Contributing

Contributions and suggestions are welcome.

When contributing:

1. Explain the proposed change.
2. Describe the expected behaviour.
3. Test the change where practical.
4. Preserve existing functionality.
5. Document new dependencies and external services.
6. Respect third-party licences.
7. Never commit passwords, API keys, or private user information.

---

## 📄 License

A project licence should be selected and included in a `LICENSE` file before redistribution permissions are described.

Until a licence is added, do not assume that the project is open source or that others are automatically permitted to redistribute or modify it.

---

## 🌟 Our Vision

TravelWake aims to help passengers rest more comfortably during their journeys while remaining aware of their destinations.

Through personalized settings, location-aware assistance, and a focus on reliability, we hope to make travel less stressful for commuters and travellers around the world.

<div align="center">

### 🌍 TravelWake

**Travel Freely. Rest Easy. Arrive Confidently.**

</div>
