package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets WHERE householdId = :householdId")
    fun getBudgetsByHousehold(householdId: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<CategoryBudgetEntity>)

    @Update
    suspend fun updateBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE householdId = :householdId")
    suspend fun deleteAllBudgetsForHousehold(householdId: String)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllBudgets()
}
