package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes WHERE householdId = :householdId ORDER BY date DESC")
    fun getIncomesByHousehold(householdId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE householdId = :householdId AND receivedBy = :memberId ORDER BY date DESC")
    fun getIncomesByMember(householdId: String, memberId: String): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: String)

    @Query("DELETE FROM incomes WHERE householdId = :householdId")
    suspend fun deleteAllIncomesForHousehold(householdId: String)

    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()
}
