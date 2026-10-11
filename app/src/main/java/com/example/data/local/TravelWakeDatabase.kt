package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ActiveTripEntity::class,
        TravelHistoryEntity::class,
        SavedPlaceEntity::class,
        UserAccountEntity::class
    ],
    version = DataPreservationManager.CURRENT_DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TravelWakeDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun historyDao(): HistoryDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun userDao(): UserDao

    companion object {
        private const val TAG = "TravelWakeDatabase"
        const val DATABASE_NAME = "travelwake_database"

        @Volatile
        private var INSTANCE: TravelWakeDatabase? = null

        fun getDatabase(context: Context): TravelWakeDatabase {
            return INSTANCE ?: synchronized(this) {
                // Pre-migration safety checkpoint: ensures user data is preserved before Room opens
                DataPreservationManager.createPreMigrationBackup(context, DATABASE_NAME)

                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TravelWakeDatabase::class.java,
                        DATABASE_NAME
                    )
                    // Explicit non-destructive migrations: PRESERVE -> MIGRATE -> VALIDATE -> RECOVER
                    .addMigrations(DataPreservationManager.MIGRATION_1_2)
                    // NEVER USE DESTRUCTIVE FALLBACK - APP UPDATE != DATA RESET
                    .build()

                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Database opening failed, attempting safe restoration from backup checkpoint: ${e.message}", e)
                    // Emergency restore to prevent data loss
                    DataPreservationManager.restoreFromBackup(context, DATABASE_NAME)
                    val fallbackInstance = Room.databaseBuilder(
                        context.applicationContext,
                        TravelWakeDatabase::class.java,
                        DATABASE_NAME
                    )
                    .addMigrations(DataPreservationManager.MIGRATION_1_2)
                    .build()

                    INSTANCE = fallbackInstance
                    fallbackInstance
                }
            }
        }
    }
}
