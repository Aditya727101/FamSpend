package com.example.data

import com.example.data.dao.CategoryBudgetDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FamilyMemberDao
import com.example.data.dao.HouseholdDao
import com.example.data.dao.IncomeDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.IncomeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val familyMemberDao: FamilyMemberDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val householdDao: HouseholdDao,
    private val incomeDao: IncomeDao
) {

    fun getExpenses(householdId: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByHousehold(householdId)
    }

    fun getIncomes(householdId: String): Flow<List<IncomeEntity>> {
        return incomeDao.getIncomesByHousehold(householdId)
    }

    fun getMembers(householdId: String): Flow<List<FamilyMemberEntity>> {
        return familyMemberDao.getMembersByHousehold(householdId)
    }

    fun getBudgets(householdId: String): Flow<List<CategoryBudgetEntity>> {
        return categoryBudgetDao.getBudgetsByHousehold(householdId)
    }

    fun getHousehold(householdId: String): Flow<HouseholdEntity?> {
        return householdDao.getHouseholdById(householdId)
    }

    suspend fun addIncome(income: IncomeEntity) {
        householdDao.updateLastSynced(income.householdId, System.currentTimeMillis())
        incomeDao.insertIncome(income)
    }

    suspend fun updateIncome(income: IncomeEntity) {
        householdDao.updateLastSynced(income.householdId, System.currentTimeMillis())
        incomeDao.updateIncome(income)
    }

    suspend fun deleteIncome(income: IncomeEntity) {
        householdDao.updateLastSynced(income.householdId, System.currentTimeMillis())
        incomeDao.deleteIncome(income)
    }

    suspend fun deleteIncomeById(id: String, householdId: String) {
        householdDao.updateLastSynced(householdId, System.currentTimeMillis())
        incomeDao.deleteIncomeById(id)
    }

    suspend fun addExpense(expense: ExpenseEntity) {
        householdDao.updateLastSynced(expense.householdId, System.currentTimeMillis())
        expenseDao.insertExpense(expense)
    }

    suspend fun addExpenses(expenses: List<ExpenseEntity>) {
        if (expenses.isEmpty()) return
        householdDao.updateLastSynced(expenses.first().householdId, System.currentTimeMillis())
        expenseDao.insertExpenses(expenses)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        householdDao.updateLastSynced(expense.householdId, System.currentTimeMillis())
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        householdDao.updateLastSynced(expense.householdId, System.currentTimeMillis())
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: String, householdId: String) {
        householdDao.updateLastSynced(householdId, System.currentTimeMillis())
        expenseDao.deleteExpenseById(id)
    }

    suspend fun addFamilyMember(member: FamilyMemberEntity) {
        familyMemberDao.insertMember(member)
    }

    suspend fun addFamilyMembers(members: List<FamilyMemberEntity>) {
        familyMemberDao.insertMembers(members)
    }

    suspend fun updateFamilyMember(member: FamilyMemberEntity) {
        familyMemberDao.updateMember(member)
    }

    suspend fun deleteFamilyMember(member: FamilyMemberEntity) {
        familyMemberDao.deleteMember(member)
    }

    suspend fun setActiveMember(householdId: String, memberId: String) {
        familyMemberDao.setActiveMember(householdId, memberId)
    }

    suspend fun setCategoryBudget(budget: CategoryBudgetEntity) {
        categoryBudgetDao.insertBudget(budget)
    }

    suspend fun updateHousehold(household: HouseholdEntity) {
        householdDao.insertHousehold(household)
    }

    suspend fun updateLastSynced(householdId: String) {
        householdDao.updateLastSynced(householdId, System.currentTimeMillis())
    }

    suspend fun clearAllAppData(householdId: String) {
        expenseDao.deleteAllExpensesForHousehold(householdId)
        incomeDao.deleteAllIncomesForHousehold(householdId)
        familyMemberDao.deleteAllMembersForHousehold(householdId)
        categoryBudgetDao.deleteAllBudgetsForHousehold(householdId)

        // Create 1 clean active profile
        val defaultMember = FamilyMemberEntity(
            id = "mem_1",
            name = "Me",
            role = "Primary",
            avatarColorHex = "#1E88E5",
            avatarIcon = "person",
            isCurrentActiveUser = true,
            householdId = householdId
        )
        familyMemberDao.insertMember(defaultMember)

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
        categoryBudgetDao.insertBudgets(defaultBudgets)
        householdDao.updateLastSynced(householdId, System.currentTimeMillis())
    }

    // Generate a simulated real-time live expense sync event from another family member!
    suspend fun simulateLiveFamilyExpense(
        householdId: String,
        members: List<FamilyMemberEntity>
    ): ExpenseEntity? {
        if (members.isEmpty()) return null

        // Pick a non-active or random member to simulate live activity
        val sender = members.random()
        val sampleItems = listOf(
            Triple("Groceries", "Organic Supermarket Snacks & Milk", listOf(18.50, 34.20, 52.80, 89.00)),
            Triple("Dining & Food", "Panera Bread Lunch & Coffee", listOf(14.80, 26.50, 42.00)),
            Triple("Transport & Fuel", "Shell Gas Station Refill", listOf(35.00, 48.20, 55.00)),
            Triple("Entertainment", "Streaming Movie Rental", listOf(6.99, 14.99, 19.99)),
            Triple("Utilities & Bills", "Water & Sanitation Bill", listOf(65.00, 88.50)),
            Triple("Shopping", "Pharmacy Prescriptions & Hygiene", listOf(22.40, 38.90)),
            Triple("Kids & Education", "Bookstore & School Supplies", listOf(19.99, 32.50))
        )

        val sample = sampleItems.random()
        val amount = sample.third.random()
        val category = sample.first
        val description = sample.second

        val newExpense = ExpenseEntity(
            amount = amount,
            category = category,
            description = description,
            paidByMemberId = sender.id,
            paidByMemberName = sender.name,
            splitType = if (Random.nextBoolean()) "Split Equally" else "Paid Individual",
            timestamp = System.currentTimeMillis(),
            householdId = householdId,
            paymentMethod = listOf("Credit Card", "Apple Pay", "Debit Card", "Google Pay").random(),
            note = "Synced in real-time from ${sender.name}'s phone"
        )

        addExpense(newExpense)
        return newExpense
    }
}
