package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM active_trip LIMIT 1")
    fun observeActiveTrip(): Flow<ActiveTripEntity?>

    @Query("SELECT * FROM active_trip LIMIT 1")
    suspend fun getActiveTrip(): ActiveTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveTrip(trip: ActiveTripEntity)

    @Update
    suspend fun updateActiveTrip(trip: ActiveTripEntity)

    @Query("DELETE FROM active_trip")
    suspend fun clearActiveTrip()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM travel_history ORDER BY endTimeMillis DESC")
    fun observeHistory(): Flow<List<TravelHistoryEntity>>

    @Query("SELECT * FROM travel_history ORDER BY endTimeMillis DESC LIMIT 10")
    suspend fun getRecentHistory(): List<TravelHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: TravelHistoryEntity)

    @Query("DELETE FROM travel_history WHERE id = :id")
    suspend fun deleteHistory(id: String)

    @Query("DELETE FROM travel_history")
    suspend fun clearAllHistory()
}

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY name ASC")
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places")
    suspend fun getAllSavedPlaces(): List<SavedPlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteSavedPlace(id: String)

    @Query("DELETE FROM saved_places")
    suspend fun clearSavedPlaces()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_account WHERE isLoggedIn = 1 LIMIT 1")
    fun observeLoggedInUser(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_account WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserAccountEntity?

    @Query("SELECT * FROM user_account WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserAccountEntity)

    @Query("UPDATE user_account SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE user_account SET isLoggedIn = 1 WHERE userId = :userId")
    suspend fun setLoggedIn(userId: String)

    @Query("DELETE FROM user_account WHERE userId = :userId")
    suspend fun deleteUser(userId: String)
}
