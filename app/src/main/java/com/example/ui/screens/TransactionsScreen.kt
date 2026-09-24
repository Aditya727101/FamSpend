package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.IncomeCategory
import com.example.data.model.IncomeEntity
import com.example.ui.components.CategoryHelper
import com.example.ui.components.ExpensesListEmptyState
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SwipeableExpenseItem
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryLight
import com.example.ui.theme.FamSuccess
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpenseSortOrder(val label: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    HIGHEST_AMOUNT("Highest amount"),
    LOWEST_AMOUNT("Lowest amount")
}

sealed class UnifiedTransaction {
    abstract val id: String
    abstract val timestamp: Long
    abstract val amount: Double

    data class Expense(val entity: ExpenseEntity) : UnifiedTransaction() {
        override val id: String get() = entity.id
        override val timestamp: Long get() = entity.timestamp
        override val amount: Double get() = entity.amount
    }

    data class Income(val entity: IncomeEntity) : UnifiedTransaction() {
        override val id: String get() = entity.id
        override val timestamp: Long get() = entity.date
        override val amount: Double get() = entity.amount
    }
}

data class UnifiedDateGroup(
    val title: String,
    val items: List<UnifiedTransaction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: UiState,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onDateRangeFilterChange: (String?) -> Unit = {},
    onMemberFilterChange: (String?) -> Unit = {},
    onPaymentMethodFilterChange: (String?) -> Unit = {},
    onAddExpenseClick: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onViewExpense: (ExpenseEntity) -> Unit = {},
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onExportCsvClick: () -> Unit = {},
    onDeleteIncome: (IncomeEntity) -> Unit = {}
) {
    var transactionType by remember { mutableStateOf("All") } // "All", "Expenses", "Income"
    val transactionTypes = listOf("All", "Expenses", "Income")

    var sortOrder by remember { mutableStateOf(ExpenseSortOrder.NEWEST_FIRST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var incomeToDelete by remember { mutableStateOf<IncomeEntity?>(null) }

    // Fast O(1) Member lookup map
    val memberMap = remember(uiState.members) {
        uiState.members.associateBy { it.id }
    }

    // Key categories for filter chips
    val filterCategoryChips = listOf(
        Pair("All", null),
        Pair("Groceries", "Groceries"),
        Pair("Bills", "Utilities & Bills"),
        Pair("Entertainment", "Entertainment"),
        Pair("Transport", "Transport & Fuel"),
        Pair("Health", "Healthcare"),
        Pair("Others", "Other Expenses")
    )

    // Filter incomes by search query and category
    val filteredIncomes = remember(uiState.incomes, uiState.searchQuery, sortOrder) {
        var list = uiState.incomes
        val q = uiState.searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            list = list.filter {
                it.source.lowercase(Locale.getDefault()).contains(q) ||
                it.category.lowercase(Locale.getDefault()).contains(q) ||
                it.receivedByName.lowercase(Locale.getDefault()).contains(q) ||
                (it.note ?: "").lowercase(Locale.getDefault()).contains(q) ||
                it.amount.toString().contains(q)
            }
        }
        when (sortOrder) {
            ExpenseSortOrder.NEWEST_FIRST -> list.sortedByDescending { it.date }
            ExpenseSortOrder.OLDEST_FIRST -> list.sortedBy { it.date }
            ExpenseSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            ExpenseSortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }
    }

    // Sort the filtered expenses
    val sortedExpenses = remember(uiState.filteredExpenses, sortOrder) {
        when (sortOrder) {
            ExpenseSortOrder.NEWEST_FIRST -> uiState.filteredExpenses.sortedByDescending { it.timestamp }
            ExpenseSortOrder.OLDEST_FIRST -> uiState.filteredExpenses.sortedBy { it.timestamp }
            ExpenseSortOrder.HIGHEST_AMOUNT -> uiState.filteredExpenses.sortedByDescending { it.amount }
            ExpenseSortOrder.LOWEST_AMOUNT -> uiState.filteredExpenses.sortedBy { it.amount }
        }
    }

    // Month income calculations for summary card
    val thisMonthIncomes = remember(uiState.incomes) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        uiState.incomes.filter { it.date >= startOfMonth }
    }
    val totalMonthIncome = remember(thisMonthIncomes) {
        thisMonthIncomes.sumOf { it.amount }
    }

    // Fast pre-calculated day boundaries for 60+ FPS date header grouping
    val (todayMidnight, yesterdayMidnight) = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis
        Pair(today, yesterday)
    }

    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }

    fun getDateHeader(timestamp: Long): String {
        return when {
            timestamp >= todayMidnight -> "Today"
            timestamp in yesterdayMidnight until todayMidnight -> "Yesterday"
            else -> dateFormatter.format(Date(timestamp))
        }
    }

    val unifiedGroups = remember(sortedExpenses, filteredIncomes, transactionType, sortOrder) {
        val list = ArrayList<UnifiedTransaction>(sortedExpenses.size + filteredIncomes.size)
        if (transactionType == "All" || transactionType == "Expenses") {
            for (e in sortedExpenses) list.add(UnifiedTransaction.Expense(e))
        }
        if (transactionType == "All" || transactionType == "Income") {
            for (i in filteredIncomes) list.add(UnifiedTransaction.Income(i))
        }

        when (sortOrder) {
            ExpenseSortOrder.NEWEST_FIRST -> list.sortByDescending { it.timestamp }
            ExpenseSortOrder.OLDEST_FIRST -> list.sortBy { it.timestamp }
            ExpenseSortOrder.HIGHEST_AMOUNT -> list.sortByDescending { it.amount }
            ExpenseSortOrder.LOWEST_AMOUNT -> list.sortBy { it.amount }
        }

        val groupsMap = LinkedHashMap<String, MutableList<UnifiedTransaction>>()
        for (item in list) {
            val header = getDateHeader(item.timestamp)
            groupsMap.getOrPut(header) { ArrayList() }.add(item)
        }

        groupsMap.map { (title, items) -> UnifiedDateGroup(title, items) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("transactions_screen")
    ) {
        item(key = "transactions_header") {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Header with Title, Count, Sort, and Export Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (transactionType == "Income") "Income History" else "Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (transactionType) {
                            "Income" -> "${filteredIncomes.size} income records"
                            "Expenses" -> "${sortedExpenses.size} expenses"
                            else -> "${sortedExpenses.size + filteredIncomes.size} records"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Button
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("expenses_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = FamPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ExpenseSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOrder == order) FamPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Export button
                    IconButton(
                        onClick = onExportCsvClick,
                        modifier = Modifier.testTag("transactions_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Report",
                            tint = FamPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented control: [All] [Expenses] [Income]
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_type_toggle")
            ) {
                transactionTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = transactionType == type,
                        onClick = { transactionType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = transactionTypes.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = if (type == "Income") FamSuccess else FamPrimary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = type,
                            fontWeight = if (transactionType == type) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Card when "Income" is selected
            if (transactionType == "Income") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("income_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FamSuccess),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Total Income This Month: ${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", totalMonthIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF14532D)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "from ${thisMonthIncomes.size} entries",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        if (transactionType == "Income") "Search income by source, member, or notes..."
                        else "Search by title, notes, or member..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (transactionType == "Income") FamSuccess else FamPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input")
            )

            // Category filter chips
            if (transactionType != "Income") {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_filter_chips")
                ) {
                    items(filterCategoryChips, key = { it.first }) { (chipLabel, catValue) ->
                        val isSelected = if (catValue == null) {
                            uiState.selectedCategoryFilter == null
                        } else {
                            catValue.equals(uiState.selectedCategoryFilter, ignoreCase = true)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) FamPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onCategoryFilterChange(if (isSelected && catValue != null) null else catValue)
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Empty state
        if (unifiedGroups.isEmpty()) {
            item(key = "transactions_empty_state") {
                if (transactionType == "Income") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "💰", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No income logged yet!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + on the home screen and choose 'Add income' to track salary, investments, and more.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    ExpensesListEmptyState(
                        onAddExpenseClick = onAddExpenseClick
                    )
                }
            }
        } else {
            // Render groups by date header
            unifiedGroups.forEach { group ->
                item(key = "header_${group.title}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                items(
                    items = group.items,
                    key = { it.id },
                    contentType = { it.javaClass }
                ) { item ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        when (item) {
                            is UnifiedTransaction.Expense -> {
                                SwipeableExpenseItem(
                                    expense = item.entity,
                                    currencySymbol = uiState.currencySymbol,
                                    onClick = { onEditExpense(item.entity) },
                                    onEditExpense = onEditExpense,
                                    onDeleteExpense = { expenseToDelete = item.entity }
                                )
                            }
                            is UnifiedTransaction.Income -> {
                                IncomeItemCard(
                                    income = item.entity,
                                    member = memberMap[item.entity.receivedBy],
                                    currencySymbol = uiState.currencySymbol,
                                    onDelete = { incomeToDelete = item.entity }
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "transactions_bottom_spacer") {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // Delete confirmation dialog for Expense
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text(
                    text = "Delete Expense?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete this expense? This will update family totals.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = expense
                        expenseToDelete = null
                        onDeleteExpense(toDelete)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FamDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog for Income
    incomeToDelete?.let { income ->
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = {
                Text(
                    text = "Delete Income?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete income entry '${income.source}'? Family totals will be updated.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = income
                        incomeToDelete = null
                        onDeleteIncome(toDelete)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FamDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IncomeItemCard(
    income: IncomeEntity,
    member: FamilyMemberEntity?,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val category = IncomeCategory.fromString(income.category)
    val dateStr = remember(income.date) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(income.date))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("income_item_${income.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green circle with Arrow Downward
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Income",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = income.source,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    // "Income" badge in green
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${category.iconEmoji} ${category.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Date received
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (income.receivedByName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Received by: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (member != null) {
                            MemberAvatar(
                                name = member.name,
                                colorHex = member.avatarColorHex,
                                iconName = member.avatarIcon,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = income.receivedByName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount in green e.g. +₹50,000
            Text(
                text = "+${currencySymbol}${String.format(Locale.US, "%,.2f", income.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF16A34A)
            )
        }
    }
}
