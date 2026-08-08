package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryBudgetDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FamilyMemberDao
import com.example.data.dao.HouseholdDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExpenseEntity::class,
        FamilyMemberEntity::class,
        CategoryBudgetEntity::class,
        HouseholdEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun householdDao(): HouseholdDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fam_expense_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultData(database)
                    }
                }
            }

            private suspend fun seedDefaultData(db: AppDatabase) {
                val householdId = "FAM-1001"
                val defaultHousehold = HouseholdEntity(
                    householdId = householdId,
                    householdName = "My Household",
                    inviteCode = "FAM-1001",
                    defaultCurrency = "$",
                    totalMonthlyBudget = 3000.0,
                    lastSyncedTimestamp = System.currentTimeMillis(),
                    isLiveSyncEnabled = true
                )
                db.householdDao().insertHousehold(defaultHousehold)

                val members = listOf(
                    FamilyMemberEntity(
                        id = "mem_1",
                        name = "Me",
                        role = "Primary",
                        avatarColorHex = "#1E88E5",
                        avatarIcon = "person",
                        isCurrentActiveUser = true,
                        householdId = householdId
                    )
                )
                db.familyMemberDao().insertMembers(members)

                val defaultBudgets = listOf(
                    CategoryBudgetEntity("Groceries", 600.0, householdId, "shopping_cart", "#4CAF50"),
                    CategoryBudgetEntity("Dining & Food", 300.0, householdId, "restaurant", "#FF9800"),
                    CategoryBudgetEntity("Utilities & Bills", 400.0, householdId, "bolt", "#2196F3"),
                    CategoryBudgetEntity("Transport & Fuel", 250.0, householdId, "directions_car", "#9C27B0"),
                    CategoryBudgetEntity("Entertainment", 200.0, householdId, "movie", "#E91E63"),
                    CategoryBudgetEntity("Kids & Education", 350.0, householdId, "school", "#00BCD4"),
                    CategoryBudgetEntity("Healthcare", 250.0, householdId, "local_hospital", "#F44336"),
                    CategoryBudgetEntity("Shopping", 200.0, householdId, "checkroom", "#795548")
                )
                db.categoryBudgetDao().insertBudgets(defaultBudgets)
            }
        }
    }
}
