package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryBudgetDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FamilyMemberDao
import com.example.data.dao.HouseholdDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.InvitationDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.IncomeEntity
import com.example.data.model.InvitationEntity
import com.example.data.model.MembershipStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExpenseEntity::class,
        FamilyMemberEntity::class,
        CategoryBudgetEntity::class,
        HouseholdEntity::class,
        IncomeEntity::class,
        InvitationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun householdDao(): HouseholdDao
    abstract fun incomeDao(): IncomeDao
    abstract fun invitationDao(): InvitationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive upgrade of family_members preserving all existing rows
                db.execSQL("ALTER TABLE family_members ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN householdRole TEXT NOT NULL DEFAULT 'MEMBER'")
                db.execSQL("ALTER TABLE family_members ADD COLUMN membershipStatus TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE family_members ADD COLUMN joinedAt INTEGER NOT NULL DEFAULT 0")
                // Promote existing primary/active member to OWNER so existing users retain full control
                db.execSQL(
                    "UPDATE family_members SET householdRole = 'OWNER' WHERE id = 'mem_1' OR isCurrentActiveUser = 1"
                )

                // Non-destructive upgrade of households
                db.execSQL("ALTER TABLE households ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")

                // Create invitations table and indexes
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS invitations (
                        id TEXT NOT NULL PRIMARY KEY,
                        householdId TEXT NOT NULL,
                        householdName TEXT NOT NULL,
                        inviterUserId TEXT NOT NULL,
                        inviterName TEXT NOT NULL,
                        inviteeEmail TEXT NOT NULL,
                        inviteeName TEXT NOT NULL,
                        personalMessage TEXT NOT NULL,
                        invitationTokenHash TEXT NOT NULL,
                        role TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        acceptedAt INTEGER,
                        acceptedByUserId TEXT NOT NULL,
                        revokedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invitations_householdId ON invitations(householdId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invitations_inviteeEmail ON invitations(inviteeEmail)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_invitations_invitationTokenHash ON invitations(invitationTokenHash)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invitations_status ON invitations(status)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fam_expense_tracker_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
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
                val now = System.currentTimeMillis()
                val defaultHousehold = HouseholdEntity(
                    householdId = householdId,
                    householdName = "My Household",
                    inviteCode = "FAM-1001",
                    defaultCurrency = "₹",
                    totalMonthlyBudget = 3000.0,
                    lastSyncedTimestamp = now,
                    isLiveSyncEnabled = true,
                    ownerUserId = ""
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
                        householdId = householdId,
                        householdRole = HouseholdRole.OWNER.value,
                        membershipStatus = MembershipStatus.ACTIVE.value,
                        joinedAt = now
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
