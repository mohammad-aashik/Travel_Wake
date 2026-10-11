package com.example.data.provider

import com.example.model.DestinationType
import com.example.model.TransitRoute
import com.example.model.TransitRouteStop
import com.example.model.TransportMode

object TransitRouteCatalog {

    val routes: List<TransitRoute> = listOf(
        // ================= SRI LANKA =================
        TransitRoute(
            id = "lk_bus_138",
            routeNumber = "138",
            name = "Homagama - Pettah Central Highway",
            mode = TransportMode.BUS,
            countryCode = "LK",
            cityName = "Colombo",
            originTerminal = "Homagama Terminal",
            destinationTerminal = "Pettah Central Bus Stand",
            frequencyDescription = "Every 3 - 5 mins",
            operatingHours = "24 Hours Service",
            description = "High-frequency trunk corridor through High Level Road via Nugegoda & Maharagama",
            majorStops = listOf(
                TransitRouteStop("lk_138_1", "Homagama Town Stand", 1, 6.8431, 80.0034, DestinationType.BUS_STAND, isMajorHub = true, "High Level Road, Homagama"),
                TransitRouteStop("lk_138_2", "Kottawa Transit Hub", 2, 6.8444, 79.9656, DestinationType.BUS_STAND, isMajorHub = true, "Makumbura Multimodal Center, Kottawa"),
                TransitRouteStop("lk_138_3", "Pannipitiya Station", 3, 6.8465, 79.9472, DestinationType.BUS_STOP, isMajorHub = false, "High Level Rd, Pannipitiya"),
                TransitRouteStop("lk_138_4", "Maharagama Clock Tower", 4, 6.8488, 79.9270, DestinationType.BUS_STAND, isMajorHub = true, "Maharagama Junction"),
                TransitRouteStop("lk_138_5", "Delkanda Junction", 5, 6.8622, 79.9056, DestinationType.BUS_STOP, isMajorHub = false, "Delkanda, Nugegoda"),
                TransitRouteStop("lk_138_6", "Nugegoda Supermarket", 6, 6.8724, 79.8974, DestinationType.BUS_STAND, isMajorHub = true, "Nugegoda Town"),
                TransitRouteStop("lk_138_7", "Kirulapone Junction", 7, 6.8790, 79.8814, DestinationType.BUS_STOP, isMajorHub = false, "Kirulapone, Colombo 06"),
                TransitRouteStop("lk_138_8", "Havelock Town", 8, 6.8850, 79.8690, DestinationType.BUS_STOP, isMajorHub = false, "Havelock Road"),
                TransitRouteStop("lk_138_9", "Thunmulla Junction", 9, 6.8972, 79.8617, DestinationType.BUS_STOP, isMajorHub = false, "Bambalapitiya Junction"),
                TransitRouteStop("lk_138_10", "Town Hall & Viharamahadevi", 10, 6.9147, 79.8653, DestinationType.LANDMARK, isMajorHub = true, "Town Hall, Colombo 07"),
                TransitRouteStop("lk_138_11", "Colombo Fort Railway Station", 11, 6.9344, 79.8503, DestinationType.RAILWAY_STATION, isMajorHub = true, "Olcott Mawatha, Colombo 01"),
                TransitRouteStop("lk_138_12", "Pettah Central Bus Stand", 12, 6.9360, 79.8530, DestinationType.BUS_STAND, isMajorHub = true, "Bastian Mawatha, Pettah")
            )
        ),
        TransitRoute(
            id = "lk_bus_100",
            routeNumber = "100",
            name = "Panadura - Pettah Coastal Highway",
            mode = TransportMode.BUS,
            countryCode = "LK",
            cityName = "Colombo",
            originTerminal = "Panadura Bus Stand",
            destinationTerminal = "Pettah Central Bus Stand",
            frequencyDescription = "Every 4 - 6 mins",
            operatingHours = "04:30 - 23:45",
            description = "Galle Road coastal trunk connecting Moratuwa, Mount Lavinia, Wellawatte and Galle Face",
            majorStops = listOf(
                TransitRouteStop("lk_100_1", "Panadura Central Stand", 1, 6.7134, 79.9074, DestinationType.BUS_STAND, isMajorHub = true, "Main Street, Panadura"),
                TransitRouteStop("lk_100_2", "Moratuwa Town", 2, 6.7731, 79.8816, DestinationType.BUS_STOP, isMajorHub = true, "Galle Rd, Moratuwa"),
                TransitRouteStop("lk_100_3", "Ratmalana Airport Junction", 3, 6.8217, 79.8765, DestinationType.BUS_STOP, isMajorHub = false, "Ratmalana"),
                TransitRouteStop("lk_100_4", "Mount Lavinia Beach Road", 4, 6.8385, 79.8654, DestinationType.LANDMARK, isMajorHub = true, "Hotel Rd, Mount Lavinia"),
                TransitRouteStop("lk_100_5", "Dehiwala Zoo Junction", 5, 6.8524, 79.8647, DestinationType.BUS_STOP, isMajorHub = true, "Galle Rd, Dehiwala"),
                TransitRouteStop("lk_100_6", "Wellawatte Manning Place", 6, 6.8744, 79.8601, DestinationType.BUS_STOP, isMajorHub = true, "Wellawatte, Colombo 06"),
                TransitRouteStop("lk_100_7", "Bambalapitiya Junction", 7, 6.8920, 79.8550, DestinationType.BUS_STOP, isMajorHub = true, "Bambalapitiya, Colombo 04"),
                TransitRouteStop("lk_100_8", "Kollupitiya Supermarket", 8, 6.9110, 79.8510, DestinationType.BUS_STOP, isMajorHub = true, "Kollupitiya, Colombo 03"),
                TransitRouteStop("lk_100_9", "Galle Face Green Promenade", 9, 6.9247, 79.8452, DestinationType.LANDMARK, isMajorHub = true, "Galle Face, Colombo 01"),
                TransitRouteStop("lk_100_10", "Pettah Central Bus Stand", 10, 6.9360, 79.8530, DestinationType.BUS_STAND, isMajorHub = true, "Olcott Mawatha, Pettah")
            )
        ),
        TransitRoute(
            id = "lk_train_main",
            routeNumber = "Main Line Train",
            name = "Colombo Fort - Kandy Intercity Express",
            mode = TransportMode.TRAIN,
            countryCode = "LK",
            cityName = "Kandy / Colombo",
            originTerminal = "Colombo Fort Station",
            destinationTerminal = "Kandy Railway Station",
            frequencyDescription = "8 Express services daily",
            operatingHours = "05:55 - 19:30",
            description = "Scenic hill country railway route traversing Ragama, Gampaha, Rambukkana, Kadugannawa and Peradeniya",
            majorStops = listOf(
                TransitRouteStop("lk_train_1", "Colombo Fort Railway Station", 1, 6.9344, 79.8503, DestinationType.RAILWAY_STATION, isMajorHub = true, "Colombo Fort"),
                TransitRouteStop("lk_train_2", "Maradana Railway Station", 2, 6.9284, 79.8647, DestinationType.RAILWAY_STATION, isMajorHub = true, "Maradana, Colombo 10"),
                TransitRouteStop("lk_train_3", "Ragama Railway Junction", 3, 7.0256, 79.9192, DestinationType.RAILWAY_STATION, isMajorHub = true, "Ragama"),
                TransitRouteStop("lk_train_4", "Gampaha Railway Station", 4, 7.0911, 79.9998, DestinationType.RAILWAY_STATION, isMajorHub = true, "Station Rd, Gampaha"),
                TransitRouteStop("lk_train_5", "Veyangoda Interchange", 5, 7.1511, 80.0578, DestinationType.RAILWAY_STATION, isMajorHub = true, "Veyangoda"),
                TransitRouteStop("lk_train_6", "Polgahawela Junction", 6, 7.3328, 80.2972, DestinationType.RAILWAY_STATION, isMajorHub = true, "Polgahawela"),
                TransitRouteStop("lk_train_7", "Rambukkana Station", 7, 7.3236, 80.3956, DestinationType.RAILWAY_STATION, isMajorHub = false, "Rambukkana"),
                TransitRouteStop("lk_train_8", "Kadugannawa Incline", 8, 7.2536, 80.5231, DestinationType.RAILWAY_STATION, isMajorHub = false, "Kadugannawa"),
                TransitRouteStop("lk_train_9", "Peradeniya Junction", 9, 7.2678, 80.5961, DestinationType.RAILWAY_STATION, isMajorHub = true, "Peradeniya"),
                TransitRouteStop("lk_train_10", "Kandy Railway Station", 10, 7.2906, 80.6289, DestinationType.RAILWAY_STATION, isMajorHub = true, "Kandy City Terminal")
            )
        ),
        TransitRoute(
            id = "lk_train_coastal",
            routeNumber = "Coastal Line Express",
            name = "Colombo Fort - Galle - Matara Samudra Devi",
            mode = TransportMode.TRAIN,
            countryCode = "LK",
            cityName = "Southern Coast",
            originTerminal = "Colombo Fort Station",
            destinationTerminal = "Matara Railway Station",
            frequencyDescription = "Hourly coastal trains",
            operatingHours = "05:30 - 20:00",
            description = "Oceanfront railway line along the Indian ocean via Mount Lavinia, Kalutara, Aluthgama, Hikkaduwa & Galle",
            majorStops = listOf(
                TransitRouteStop("lk_c_1", "Colombo Fort Station", 1, 6.9344, 79.8503, DestinationType.RAILWAY_STATION, isMajorHub = true, "Colombo Fort"),
                TransitRouteStop("lk_c_2", "Mount Lavinia Station", 2, 6.8385, 79.8654, DestinationType.RAILWAY_STATION, isMajorHub = true, "Mount Lavinia Beach"),
                TransitRouteStop("lk_c_3", "Panadura Station", 3, 6.7134, 79.9074, DestinationType.RAILWAY_STATION, isMajorHub = true, "Panadura Town"),
                TransitRouteStop("lk_c_4", "Kalutara South Station", 4, 6.5819, 79.9603, DestinationType.RAILWAY_STATION, isMajorHub = true, "Kalutara Bodhiya"),
                TransitRouteStop("lk_c_5", "Aluthgama / Bentota Station", 5, 6.4297, 80.0019, DestinationType.RAILWAY_STATION, isMajorHub = true, "Aluthgama"),
                TransitRouteStop("lk_c_6", "Ambalangoda Station", 6, 6.2367, 80.0531, DestinationType.RAILWAY_STATION, isMajorHub = false, "Ambalangoda"),
                TransitRouteStop("lk_c_7", "Hikkaduwa Surf Beach", 7, 6.1417, 80.1017, DestinationType.RAILWAY_STATION, isMajorHub = true, "Hikkaduwa"),
                TransitRouteStop("lk_c_8", "Galle Railway Station", 8, 6.0353, 80.2158, DestinationType.RAILWAY_STATION, isMajorHub = true, "Galle Fort"),
                TransitRouteStop("lk_c_9", "Weligama Bay Station", 9, 5.9722, 80.4286, DestinationType.RAILWAY_STATION, isMajorHub = true, "Weligama"),
                TransitRouteStop("lk_c_10", "Matara Railway Terminal", 10, 5.9482, 80.5488, DestinationType.RAILWAY_STATION, isMajorHub = true, "Matara City")
            )
        ),
        TransitRoute(
            id = "lk_bus_177",
            routeNumber = "177",
            name = "Kollupitiya - Kaduwela Parliament Line",
            mode = TransportMode.BUS,
            countryCode = "LK",
            cityName = "Colombo",
            originTerminal = "Kollupitiya Supermarket",
            destinationTerminal = "Kaduwela Bus Stand",
            frequencyDescription = "Every 5 - 8 mins",
            operatingHours = "05:00 - 23:00",
            description = "Capital corridor passing through Rajagiriya, Battaramulla, Sri Jayawardenepura Kotte & Malabe IT Hub",
            majorStops = listOf(
                TransitRouteStop("lk_177_1", "Kollupitiya Station", 1, 6.9110, 79.8510, DestinationType.BUS_STAND, isMajorHub = true, "Kollupitiya"),
                TransitRouteStop("lk_177_2", "Town Hall / Nelum Pokuna", 2, 6.9119, 79.8644, DestinationType.LANDMARK, isMajorHub = true, "Albert Crescent"),
                TransitRouteStop("lk_177_3", "Borella Junction", 3, 6.9150, 79.8778, DestinationType.BUS_STAND, isMajorHub = true, "Borella, Colombo 08"),
                TransitRouteStop("lk_177_4", "Rajagiriya Flyover", 4, 6.9083, 79.8942, DestinationType.BUS_STOP, isMajorHub = true, "Rajagiriya"),
                TransitRouteStop("lk_177_5", "Battaramulla Suhurupaya", 5, 6.8972, 79.9194, DestinationType.WORKPLACE, isMajorHub = true, "Passport & ID Office, Battaramulla"),
                TransitRouteStop("lk_177_6", "Malabe Junction (SLIIT Hub)", 6, 6.9039, 79.9575, DestinationType.UNIVERSITY, isMajorHub = true, "Malabe Tech Corridor"),
                TransitRouteStop("lk_177_7", "Kaduwela Expressway Interchange", 7, 6.9367, 79.9833, DestinationType.BUS_STAND, isMajorHub = true, "Kaduwela Town")
            )
        ),

        // ================= UNITED STATES =================
        TransitRoute(
            id = "us_subway_1",
            routeNumber = "Line 1 Subway",
            name = "Broadway - 7th Avenue Local",
            mode = TransportMode.METRO,
            countryCode = "US",
            cityName = "New York",
            originTerminal = "Van Cortlandt Park - 242 St",
            destinationTerminal = "South Ferry Terminal",
            frequencyDescription = "Every 3 - 6 mins",
            operatingHours = "24 Hours Service",
            description = "Iconic Manhattan west side artery connecting Columbia Univ, Times Square, Penn Station and WTC",
            majorStops = listOf(
                TransitRouteStop("us_1_1", "168 St - Washington Heights", 1, 40.8407, -73.9396, DestinationType.METRO_STATION, isMajorHub = true, "Broadway & 168th St"),
                TransitRouteStop("us_1_2", "116 St - Columbia University", 2, 40.8077, -73.9641, DestinationType.UNIVERSITY, isMajorHub = true, "Broadway & 116th St"),
                TransitRouteStop("us_1_3", "72 St Station", 3, 40.7784, -73.9820, DestinationType.METRO_STATION, isMajorHub = true, "Broadway & 72nd St"),
                TransitRouteStop("us_1_4", "Times Square - 42 St", 4, 40.7580, -73.9855, DestinationType.METRO_STATION, isMajorHub = true, "7th Ave & 42nd St"),
                TransitRouteStop("us_1_5", "34 St - Penn Station", 5, 40.7505, -73.9934, DestinationType.RAILWAY_STATION, isMajorHub = true, "7th Ave & 34th St"),
                TransitRouteStop("us_1_6", "14 St / 6th Ave", 6, 40.7378, -73.9998, DestinationType.METRO_STATION, isMajorHub = true, "7th Ave & 14th St"),
                TransitRouteStop("us_1_7", "WTC Cortlandt", 7, 40.7118, -74.0121, DestinationType.METRO_STATION, isMajorHub = true, "World Trade Center"),
                TransitRouteStop("us_1_8", "South Ferry", 8, 40.7014, -74.0132, DestinationType.FERRY_TERMINAL, isMajorHub = true, "Battery Park, Manhattan")
            )
        ),
        TransitRoute(
            id = "us_subway_a",
            routeNumber = "Line A Express",
            name = "8th Avenue Express / JFK Airport Link",
            mode = TransportMode.METRO,
            countryCode = "US",
            cityName = "New York",
            originTerminal = "Inwood - 207 St",
            destinationTerminal = "Far Rockaway / JFK Airtrain",
            frequencyDescription = "Every 4 - 8 mins",
            operatingHours = "24 Hours Service",
            description = "Longest single-seat metro route in the USA spanning Bronx, Manhattan, Brooklyn & Queens",
            majorStops = listOf(
                TransitRouteStop("us_a_1", "Columbus Circle - 59 St", 1, 40.7682, -73.9819, DestinationType.METRO_STATION, isMajorHub = true, "Central Park South"),
                TransitRouteStop("us_a_2", "Port Authority Bus Terminal", 2, 40.7573, -73.9897, DestinationType.BUS_STAND, isMajorHub = true, "8th Ave & 42nd St"),
                TransitRouteStop("us_a_3", "Penn Station - 34 St", 3, 40.7505, -73.9934, DestinationType.RAILWAY_STATION, isMajorHub = true, "8th Ave & 34th St"),
                TransitRouteStop("us_a_4", "Fulton St Transit Center", 4, 40.7103, -74.0095, DestinationType.METRO_STATION, isMajorHub = true, "Broadway & Fulton"),
                TransitRouteStop("us_a_5", "Jay St - MetroTech", 5, 40.6923, -73.9873, DestinationType.METRO_STATION, isMajorHub = true, "Downtown Brooklyn"),
                TransitRouteStop("us_a_6", "Howard Beach - JFK Airport Link", 6, 40.6605, -73.8306, DestinationType.AIRPORT, isMajorHub = true, "Airtrain JFK Connection")
            )
        ),

        // ================= UNITED KINGDOM =================
        TransitRoute(
            id = "uk_piccadilly",
            routeNumber = "Piccadilly Line",
            name = "Heathrow Airport - King's Cross - Cockfosters",
            mode = TransportMode.METRO,
            countryCode = "GB",
            cityName = "London",
            originTerminal = "Heathrow Terminals 2, 3, 4 & 5",
            destinationTerminal = "Cockfosters Station",
            frequencyDescription = "Every 3 - 5 mins",
            operatingHours = "24hr Night Tube on weekends",
            description = "Direct London Tube connecting Heathrow Airport, South Kensington, Piccadilly Circus & King's Cross",
            majorStops = listOf(
                TransitRouteStop("uk_p_1", "Heathrow Airport Terminal 2 & 3", 1, 51.4700, -0.4543, DestinationType.AIRPORT, isMajorHub = true, "Heathrow Central"),
                TransitRouteStop("uk_p_2", "Hammersmith Broadway", 2, 51.4925, -0.2244, DestinationType.METRO_STATION, isMajorHub = true, "Hammersmith"),
                TransitRouteStop("uk_p_3", "South Kensington Museums", 3, 51.4941, -0.1738, DestinationType.LANDMARK, isMajorHub = true, "Science & V&A Museums"),
                TransitRouteStop("uk_p_4", "Hyde Park Corner", 4, 51.5030, -0.1506, DestinationType.METRO_STATION, isMajorHub = false, "Knightsbridge"),
                TransitRouteStop("uk_p_5", "Piccadilly Circus", 5, 51.5100, -0.1342, DestinationType.METRO_STATION, isMajorHub = true, "West End Heart"),
                TransitRouteStop("uk_p_6", "Covent Garden", 6, 51.5129, -0.1243, DestinationType.METRO_STATION, isMajorHub = true, "Covent Garden"),
                TransitRouteStop("uk_p_7", "King's Cross St. Pancras", 7, 51.5308, -0.1238, DestinationType.RAILWAY_STATION, isMajorHub = true, "Euston Road Intercity Hub"),
                TransitRouteStop("uk_p_8", "Finsbury Park Interchange", 8, 51.5642, -0.1064, DestinationType.METRO_STATION, isMajorHub = true, "North London Hub")
            )
        ),

        // ================= INDIA =================
        TransitRoute(
            id = "in_delhi_yellow",
            routeNumber = "Yellow Line Metro",
            name = "Samaypur Badli - New Delhi - Millennium City Gurugram",
            mode = TransportMode.METRO,
            countryCode = "IN",
            cityName = "Delhi NCR",
            originTerminal = "Samaypur Badli",
            destinationTerminal = "Millennium City Centre Gurugram",
            frequencyDescription = "Every 2 - 4 mins",
            operatingHours = "05:30 - 23:30",
            description = "Busiest transit line in Delhi linking Old Delhi, New Delhi Railway Station, Connaught Place & Cyber City",
            majorStops = listOf(
                TransitRouteStop("in_y_1", "Chandni Chowk", 1, 28.6578, 77.2300, DestinationType.METRO_STATION, isMajorHub = true, "Old Delhi"),
                TransitRouteStop("in_y_2", "New Delhi Railway Station Metro", 2, 28.6427, 77.2195, DestinationType.RAILWAY_STATION, isMajorHub = true, "Bhavbhuti Marg"),
                TransitRouteStop("in_y_3", "Rajiv Chowk (Connaught Place)", 3, 28.6328, 77.2197, DestinationType.METRO_STATION, isMajorHub = true, "CP Central Interchange"),
                TransitRouteStop("in_y_4", "Central Secretariat", 4, 28.6146, 77.2119, DestinationType.METRO_STATION, isMajorHub = true, "Govt Offices & India Gate"),
                TransitRouteStop("in_y_5", "Hauz Khas Junction", 5, 28.5434, 77.2064, DestinationType.METRO_STATION, isMajorHub = true, "IIT & South Delhi"),
                TransitRouteStop("in_y_6", "Cyber City / Sikanderpur", 6, 28.4815, 77.0927, DestinationType.METRO_STATION, isMajorHub = true, "Gurugram Tech Park")
            )
        ),

        // ================= JAPAN =================
        TransitRoute(
            id = "jp_yamanote",
            routeNumber = "JR Yamanote Line",
            name = "Tokyo Loop Circular Railway",
            mode = TransportMode.TRAIN,
            countryCode = "JP",
            cityName = "Tokyo",
            originTerminal = "Tokyo Station (Loop)",
            destinationTerminal = "Shinjuku / Shibuya (Loop)",
            frequencyDescription = "Every 2 - 3 mins",
            operatingHours = "04:30 - 01:15",
            description = "Famous continuous loop connecting all major Tokyo city centres and high-speed Shinkansen lines",
            majorStops = listOf(
                TransitRouteStop("jp_y_1", "Tokyo Station (Shinkansen Hub)", 1, 35.6812, 139.7671, DestinationType.RAILWAY_STATION, isMajorHub = true, "Chiyoda City, Tokyo"),
                TransitRouteStop("jp_y_2", "Akihabara Electric Town", 2, 35.6983, 139.7731, DestinationType.RAILWAY_STATION, isMajorHub = true, "Akihabara"),
                TransitRouteStop("jp_y_3", "Ueno Station & Park", 3, 35.7138, 139.7772, DestinationType.RAILWAY_STATION, isMajorHub = true, "Taito City"),
                TransitRouteStop("jp_y_4", "Ikebukuro Mega Interchange", 4, 35.7295, 139.7109, DestinationType.RAILWAY_STATION, isMajorHub = true, "Ikebukuro"),
                TransitRouteStop("jp_y_5", "Shinjuku Station (World's Busiest)", 5, 35.6896, 139.7006, DestinationType.RAILWAY_STATION, isMajorHub = true, "Shinjuku Ward"),
                TransitRouteStop("jp_y_6", "Harajuku / Meiji Jingu", 6, 35.6702, 139.7027, DestinationType.RAILWAY_STATION, isMajorHub = false, "Takeshita Street"),
                TransitRouteStop("jp_y_7", "Shibuya Scramble Crossing", 7, 35.6580, 139.7016, DestinationType.RAILWAY_STATION, isMajorHub = true, "Shibuya Station Square"),
                TransitRouteStop("jp_y_8", "Shinagawa Shinkansen Station", 8, 35.6285, 139.7388, DestinationType.RAILWAY_STATION, isMajorHub = true, "Minato City")
            )
        )
    )

    /**
     * Search routes matching a query, prioritizing current active country
     */
    fun searchRoutes(
        query: String,
        preferredCountryCode: String? = null,
        searchWorldwide: Boolean = false
    ): List<TransitRoute> {
        val q = query.trim().lowercase()
        val cCode = preferredCountryCode?.trim()?.uppercase()

        val candidateList = if (!searchWorldwide && !cCode.isNullOrEmpty()) {
            val inCountry = routes.filter { it.countryCode.equals(cCode, ignoreCase = true) }
            val outside = routes.filter { !it.countryCode.equals(cCode, ignoreCase = true) }
            inCountry + outside
        } else {
            routes
        }

        if (q.isEmpty()) {
            return candidateList.take(6)
        }

        return candidateList.filter { route ->
            route.routeNumber.lowercase().contains(q) ||
            route.name.lowercase().contains(q) ||
            route.cityName.lowercase().contains(q) ||
            route.originTerminal.lowercase().contains(q) ||
            route.destinationTerminal.lowercase().contains(q) ||
            route.mode.label.lowercase().contains(q) ||
            route.majorStops.any { stop ->
                stop.name.lowercase().contains(q) ||
                stop.fullAddress.lowercase().contains(q)
            }
        }
    }
}
