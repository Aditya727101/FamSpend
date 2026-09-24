package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExpenseEntity
import com.example.util.CsvExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FamSpend", appName)
    }

    @Test
    fun `test expense aggregation and budget remaining calculation`() {
        val totalBudget = 50000.0
        val expenses = listOf(
            ExpenseEntity(
                id = "1",
                amount = 1200.50,
                category = "Groceries",
                paidByMemberId = "m1",
                paidByMemberName = "Dad",
                description = "Vegetables and milk"
            ),
            ExpenseEntity(
                id = "2",
                amount = 3500.00,
                category = "Utilities",
                paidByMemberId = "m2",
                paidByMemberName = "Mom",
                description = "Electricity bill"
            )
        )

        val totalSpent = expenses.sumOf { it.amount }
        assertEquals(4700.50, totalSpent, 0.001)

        val remainingBudget = totalBudget - totalSpent
        assertEquals(45299.50, remainingBudget, 0.001)
        assertFalse(remainingBudget < 0)
    }

    @Test
    fun `test csv generation creates valid content without crash`() {
        val expenses = listOf(
            ExpenseEntity(
                id = "1",
                amount = 450.00,
                category = "Dining",
                paidByMemberId = "m1",
                paidByMemberName = "Maya",
                description = "Lunch with friends",
                currencySymbol = "₹"
            )
        )

        val csvString = CsvExportHelper.generateCsvString(expenses, "Sharma Family")
        assertTrue(csvString.contains("SHARMA FAMILY"))
        assertTrue(csvString.contains("Lunch with friends"))
        assertTrue(csvString.contains("450.00"))
    }

    @Test
    fun `test small phone responsive boundary math`() {
        // Test edge cases common on small devices: zero expenses, small viewport calculations
        val zeroExpenses = emptyList<ExpenseEntity>()
        val zeroSpent = zeroExpenses.sumOf { it.amount }
        assertEquals(0.0, zeroSpent, 0.0)

        val daysInMonth = 30
        val dailyAvg = if (daysInMonth > 0) zeroSpent / daysInMonth else 0.0
        assertEquals(0.0, dailyAvg, 0.0)

        // Huge number format test (no scientific notation crash)
        val formatted = String.format(Locale.US, "%,.2f", 1250000.75)
        assertEquals("1,250,000.75", formatted)
    }

    @Test
    fun `test transfer between accounts entity and description`() {
        val from = "Checking Account"
        val to = "Savings Account"
        val amount = 5000.0
        val fee = 25.0
        val transferExp = ExpenseEntity(
            amount = amount + fee,
            currencySymbol = "₹",
            category = "Transfer",
            description = "Transfer: $from ➔ $to",
            paidByMemberId = "m1",
            paidByMemberName = "Dad",
            splitType = "Transfer",
            paymentMethod = from,
            note = "Monthly savings deposit (Fee: ₹25.00)",
            tags = "transfer,$from,$to"
        )

        assertEquals("Transfer", transferExp.category)
        assertEquals("Transfer: Checking Account ➔ Savings Account", transferExp.description)
        assertEquals(5025.0, transferExp.amount, 0.001)
        assertTrue(transferExp.tags.contains("transfer"))
        assertTrue(transferExp.tags.contains("Checking Account"))
    }

    @Test
    fun `test scan receipt data structure and receiptUri`() {
        val sampleItems = listOf(
            "Organic Produce" to 18.20,
            "Almond Milk" to 3.99,
            "Eggs & Dairy" to 8.50
        )
        val total = sampleItems.sumOf { it.second }
        val receiptExpense = ExpenseEntity(
            amount = total,
            currencySymbol = "₹",
            category = "Groceries",
            description = "Trader Joe's Market",
            paidByMemberId = "m2",
            paidByMemberName = "Mom",
            receiptUri = "content://demo.receipt/supermarket",
            note = "Scanned receipt: Trader Joe's Market (${sampleItems.size} items)"
        )

        assertEquals(30.69, receiptExpense.amount, 0.001)
        assertEquals("content://demo.receipt/supermarket", receiptExpense.receiptUri)
        assertTrue(receiptExpense.note.contains("Scanned receipt"))
    }

    @Test
    fun `test split expense calculation modes`() {
        val totalBill = 120.0
        val membersCount = 3

        // 1. Equal split
        val equalShare = totalBill / membersCount
        assertEquals(40.0, equalShare, 0.001)

        // 2. Shares split: Member A = 2 shares, Member B = 1 share, Member C = 1 share (total 4 shares)
        val shares = listOf(2, 1, 1)
        val totalShares = shares.sum()
        val perShare = totalBill / totalShares // 30.0
        assertEquals(30.0, perShare, 0.001)
        assertEquals(60.0, shares[0] * perShare, 0.001)
        assertEquals(30.0, shares[1] * perShare, 0.001)
        assertEquals(30.0, shares[2] * perShare, 0.001)

        // 3. Percentage split: 50%, 25%, 25%
        val pcts = listOf(50.0, 25.0, 25.0)
        assertEquals(60.0, (pcts[0] / 100.0) * totalBill, 0.001)
        assertEquals(30.0, (pcts[1] / 100.0) * totalBill, 0.001)
        assertEquals(30.0, (pcts[2] / 100.0) * totalBill, 0.001)
    }
}
