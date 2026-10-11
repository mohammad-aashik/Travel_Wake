package com.example.model

import android.content.Context
import java.util.Locale

data class Country(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val defaultCity: String,
    val popularInApp: Boolean = false,
    val sampleHubs: List<String> = emptyList()
)

object CountryData {
    val allCountries = listOf(
        // Popular / Primary Choices at top
        Country(
            code = "LK",
            name = "Sri Lanka",
            flagEmoji = "🇱🇰",
            defaultCity = "Colombo",
            popularInApp = true,
            sampleHubs = listOf("Colombo Fort", "Kandy", "Galle", "Jaffna", "Negombo", "Pettah")
        ),
        Country(
            code = "IN",
            name = "India",
            flagEmoji = "🇮🇳",
            defaultCity = "New Delhi",
            popularInApp = true,
            sampleHubs = listOf("New Delhi Station", "Chennai Central", "Mumbai CSMT", "Bengaluru Majestic")
        ),
        Country(
            code = "GB",
            name = "United Kingdom",
            flagEmoji = "🇬🇧",
            defaultCity = "London",
            popularInApp = true,
            sampleHubs = listOf("King's Cross", "Victoria Station", "Paddington", "Waterloo")
        ),
        Country(
            code = "SG",
            name = "Singapore",
            flagEmoji = "🇸🇬",
            defaultCity = "Singapore",
            popularInApp = true,
            sampleHubs = listOf("Changi Airport", "Dhoby Ghaut MRT", "Marina Bay", "Jurong East")
        ),
        Country(
            code = "AU",
            name = "Australia",
            flagEmoji = "🇦🇺",
            defaultCity = "Sydney",
            popularInApp = true,
            sampleHubs = listOf("Sydney Central", "Flinders Street Melbourne", "Roma Street Brisbane")
        ),
        Country(
            code = "US",
            name = "United States",
            flagEmoji = "🇺🇸",
            defaultCity = "New York",
            popularInApp = true,
            sampleHubs = listOf("Penn Station", "Grand Central", "Chicago Union", "San Francisco Transbay")
        ),

        // Global Countries
        Country(code = "JP", name = "Japan", flagEmoji = "🇯🇵", defaultCity = "Tokyo", sampleHubs = listOf("Tokyo Station", "Shinjuku")),
        Country(code = "DE", name = "Germany", flagEmoji = "🇩🇪", defaultCity = "Berlin", sampleHubs = listOf("Berlin Hbf", "München Hbf")),
        Country(code = "FR", name = "France", flagEmoji = "🇫🇷", defaultCity = "Paris", sampleHubs = listOf("Gare du Nord", "Gare de Lyon")),
        Country(code = "CA", name = "Canada", flagEmoji = "🇨🇦", defaultCity = "Toronto", sampleHubs = listOf("Union Station Toronto", "Central Station Montreal")),
        Country(code = "AE", name = "United Arab Emirates", flagEmoji = "🇦🇪", defaultCity = "Dubai", sampleHubs = listOf("Dubai Mall Metro", "Union Metro")),
        Country(code = "MY", name = "Malaysia", flagEmoji = "🇲🇾", defaultCity = "Kuala Lumpur", sampleHubs = listOf("KL Sentral", "Bandar Tasik Selatan")),
        Country(code = "TH", name = "Thailand", flagEmoji = "🇹🇭", defaultCity = "Bangkok", sampleHubs = listOf("Krung Thep Aphiwat", "Siam BTS")),
        Country(code = "CH", name = "Switzerland", flagEmoji = "🇨🇭", defaultCity = "Zurich", sampleHubs = listOf("Zürich HB", "Genève Cornavin")),
        Country(code = "IT", name = "Italy", flagEmoji = "🇮🇹", defaultCity = "Rome", sampleHubs = listOf("Roma Termini", "Milano Centrale")),
        Country(code = "ES", name = "Spain", flagEmoji = "🇪🇸", defaultCity = "Madrid", sampleHubs = listOf("Madrid Atocha", "Barcelona Sants")),
        Country(code = "NL", name = "Netherlands", flagEmoji = "🇳🇱", defaultCity = "Amsterdam", sampleHubs = listOf("Amsterdam Centraal", "Utrecht Centraal")),
        Country(code = "NZ", name = "New Zealand", flagEmoji = "🇳🇿", defaultCity = "Auckland", sampleHubs = listOf("Britomart Auckland", "Wellington Station")),
        Country(code = "KR", name = "South Korea", flagEmoji = "🇰🇷", defaultCity = "Seoul", sampleHubs = listOf("Seoul Station", "Busan Station")),
        Country(code = "ZA", name = "South Africa", flagEmoji = "🇿🇦", defaultCity = "Cape Town", sampleHubs = listOf("Cape Town Station", "Park Station Johannesburg")),
        Country(code = "BR", name = "Brazil", flagEmoji = "🇧🇷", defaultCity = "São Paulo", sampleHubs = listOf("Estação da Sé", "Central do Brasil")),
        Country(code = "ID", name = "Indonesia", flagEmoji = "🇮🇩", defaultCity = "Jakarta", sampleHubs = listOf("Gambir Station", "Manggarai")),
        Country(code = "PH", name = "Philippines", flagEmoji = "🇵🇭", defaultCity = "Manila", sampleHubs = listOf("EDSA Station", "Tutuban")),
        Country(code = "SA", name = "Saudi Arabia", flagEmoji = "🇸🇦", defaultCity = "Riyadh", sampleHubs = listOf("Riyadh Metro Hub", "Haramain High Speed")),
        Country(code = "PK", name = "Pakistan", flagEmoji = "🇵🇰", defaultCity = "Karachi", sampleHubs = listOf("Karachi Cantonment", "Lahore Junction")),
        Country(code = "BD", name = "Bangladesh", flagEmoji = "🇧🇩", defaultCity = "Dhaka", sampleHubs = listOf("Kamalapur Railway Station", "Dhaka Metro")),
        Country(code = "VN", name = "Vietnam", flagEmoji = "🇻🇳", defaultCity = "Hanoi", sampleHubs = listOf("Hanoi Railway Station", "Ben Thanh")),
        Country(code = "TR", name = "Turkey", flagEmoji = "🇹🇷", defaultCity = "Istanbul", sampleHubs = listOf("Marmaray Sirkeci", "Yenikapi")),
        Country(code = "SE", name = "Sweden", flagEmoji = "🇸🇪", defaultCity = "Stockholm", sampleHubs = listOf("Stockholm Central", "Göteborg Central")),
        Country(code = "NO", name = "Norway", flagEmoji = "🇳🇴", defaultCity = "Oslo", sampleHubs = listOf("Oslo Sentralstasjon", "Bergen")),
        Country(code = "IE", name = "Ireland", flagEmoji = "🇮🇪", defaultCity = "Dublin", sampleHubs = listOf("Dublin Connolly", "Heuston Station")),
        Country(code = "EG", name = "Egypt", flagEmoji = "🇪🇬", defaultCity = "Cairo", sampleHubs = listOf("Ramses Station", "Sadat Metro"))
    )

    val defaultCountry = allCountries.first() // Sri Lanka (LK)

    fun findByCode(code: String): Country {
        return allCountries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: defaultCountry
    }

    fun searchCountries(query: String): List<Country> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return allCountries.distinctBy { it.code }
        return allCountries.filter {
            it.name.lowercase().contains(trimmed) ||
            it.code.lowercase().contains(trimmed) ||
            it.defaultCity.lowercase().contains(trimmed)
        }.distinctBy { it.code }
    }

    /**
     * Attempts to detect country from system locale or telephony.
     */
    fun detectDeviceCountry(context: Context? = null): Country {
        try {
            val localeCountry = Locale.getDefault().country
            if (!localeCountry.isNullOrBlank()) {
                val matched = allCountries.firstOrNull { it.code.equals(localeCountry, ignoreCase = true) }
                if (matched != null) return matched
            }
        } catch (_: Exception) {
            // Fall back
        }
        return defaultCountry
    }
}
