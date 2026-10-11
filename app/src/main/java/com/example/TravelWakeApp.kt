package com.example

import android.app.Application
import com.example.data.repository.TravelWakeRepository

class TravelWakeApp : Application() {
    lateinit var repository: TravelWakeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TravelWakeRepository(this)
    }
}
