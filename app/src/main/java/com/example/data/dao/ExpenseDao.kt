package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE householdId = :householdId ORDER BY timestamp DESC")
    fun getExpensesByHousehold(householdId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE householdId = :householdId AND paidByMemberId = :memberId ORDER BY timestamp DESC")
    fun getExpensesByMember(householdId: String, memberId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses WHERE householdId = :householdId")
    suspend fun deleteAllExpensesForHousehold(householdId: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
