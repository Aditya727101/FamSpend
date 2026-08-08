package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HouseholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {

    @Query("SELECT * FROM households WHERE householdId = :householdId LIMIT 1")
    fun getHouseholdById(householdId: String): Flow<HouseholdEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity)

    @Update
    suspend fun updateHousehold(household: HouseholdEntity)

    @Query("UPDATE households SET lastSyncedTimestamp = :timestamp WHERE householdId = :householdId")
    suspend fun updateLastSynced(householdId: String, timestamp: Long)
}
