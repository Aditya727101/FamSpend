package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SplitMode(val label: String) {
    EQUALLY("Equally"),
    EXACT("Exact Amounts"),
    PERCENTAGE("Percentage (%)"),
    SHARES("Shares / Ratio")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SplitExpenseDialog(
    members: List<FamilyMemberEntity>,
    activeMember: FamilyMemberEntity?,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        category: String,
        description: String,
        paidByMemberId: String,
        paidByMemberName: String,
        splitType: String,
        paymentMethod: String,
        note: String,
        timestamp: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Dining & Food") }
    var selectedPaymentMethod by remember { mutableStateOf("Credit Card") }
    var noteText by remember { mutableStateOf("") }

    var splitMode by remember { mutableStateOf(SplitMode.EQUALLY) }

    // Date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // Paid by member
    val defaultPayer = activeMember ?: members.firstOrNull()
    var payerId by remember { mutableStateOf(defaultPayer?.id ?: "mem_1") }
    var payerName by remember { mutableStateOf(defaultPayer?.name ?: "Family Member") }

    // Split state tracking
    // 1. Equal split: Set of member IDs included
    val includedMemberIds = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(members) {
        if (includedMemberIds.isEmpty()) {
            members.forEach { includedMemberIds[it.id] = true }
        }
    }

    // 2. Exact split: Map of member ID -> amount text
    val exactAmounts = remember { mutableStateMapOf<String, String>() }

    // 3. Percentage split: Map of member ID -> percentage string
    val percentages = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(members) {
        if (percentages.isEmpty() && members.isNotEmpty()) {
            val equalPct = (100.0 / members.size)
            members.forEach {
                percentages[it.id] = String.format(Locale.US, "%.1f", equalPct)
            }
        }
    }

    // 4. Shares split: Map of member ID -> integer shares
    val shares = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(members) {
        if (shares.isEmpty()) {
            members.forEach { shares[it.id] = 1 }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("split_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Split Expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Divide costs equally or custom with family",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_split_expense")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Total Bill Amount ($currencySymbol)") },
                leadingIcon = {
                    Text(
                        text = currencySymbol,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_total_amount_input"),
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description / Title
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Expense Title / Purpose") },
                placeholder = { Text("e.g. Family dinner, Costco bulk run, Cabin rental") },
                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_description_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf("Dining & Food", "Groceries", "Utilities & Bills", "Entertainment", "Travel & Vacation", "Shopping")
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paid By Member
            Text(
                text = "Who Paid Upfront?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                members.forEach { member ->
                    val isSelected = member.id == payerId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                payerId = member.id
                                payerName = member.name
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            MemberAvatar(
                                name = member.name,
                                colorHex = member.avatarColorHex,
                                iconName = member.avatarIcon,
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split Mode Tabs (Equally, Exact, Percentage, Shares)
            Text(
                text = "Split Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SplitMode.entries.forEach { mode ->
                    val isSelected = mode == splitMode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                splitMode = mode
                                errorMessage = null
                            }
                            .testTag("split_mode_${mode.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split Calculation Details according to Mode
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (splitMode) {
                        SplitMode.EQUALLY -> {
                            val activeMembersCount = includedMemberIds.count { it.value }
                            val perPerson = if (activeMembersCount > 0 && totalAmount > 0) totalAmount / activeMembersCount else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Participants ($activeMembersCount selected)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%.2f", perPerson)} each",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            members.forEach { member ->
                                val isChecked = includedMemberIds[member.id] ?: true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            includedMemberIds[member.id] = !isChecked
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { includedMemberIds[member.id] = it }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        MemberAvatar(name = member.name, colorHex = member.avatarColorHex, iconName = member.avatarIcon, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = member.name + if (member.id == payerId) " (Payer)" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (member.id == payerId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    if (isChecked) {
                                        Text(
                                            text = "$currencySymbol${String.format(Locale.US, "%.2f", perPerson)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        SplitMode.EXACT -> {
                            val currentSum = members.sumOf { exactAmounts[it.id]?.toDoubleOrNull() ?: 0.0 }
                            val diff = totalAmount - currentSum

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter Exact Share per Person",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total: $currencySymbol${String.format(Locale.US, "%.2f", currentSum)} / $currencySymbol${String.format(Locale.US, "%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(diff) < 0.01) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            members.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        MemberAvatar(name = member.name, colorHex = member.avatarColorHex, iconName = member.avatarIcon, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (member.id == payerId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    OutlinedTextField(
                                        value = exactAmounts[member.id] ?: "",
                                        onValueChange = { exactAmounts[member.id] = it },
                                        placeholder = { Text("0.00") },
                                        leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                            }

                            if (diff > 0.01 && members.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        val perRemaining = diff / members.size
                                        members.forEach { m ->
                                            val current = exactAmounts[m.id]?.toDoubleOrNull() ?: 0.0
                                            exactAmounts[m.id] = String.format(Locale.US, "%.2f", current + perRemaining)
                                        }
                                    }
                                ) {
                                    Text("Distribute Remaining ($currencySymbol${String.format(Locale.US, "%.2f", diff)}) Evenly")
                                }
                            }
                        }

                        SplitMode.PERCENTAGE -> {
                            val totalPct = members.sumOf { percentages[it.id]?.toDoubleOrNull() ?: 0.0 }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assign Percentages",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", totalPct)}% / 100%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(totalPct - 100.0) < 0.5) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            members.forEach { member ->
                                val pct = percentages[member.id]?.toDoubleOrNull() ?: 0.0
                                val calculatedAmt = if (totalAmount > 0) (pct / 100.0) * totalAmount else 0.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        MemberAvatar(name = member.name, colorHex = member.avatarColorHex, iconName = member.avatarIcon, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "$currencySymbol${String.format(Locale.US, "%.2f", calculatedAmt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = percentages[member.id] ?: "",
                                        onValueChange = { percentages[member.id] = it },
                                        placeholder = { Text("0") },
                                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }

                        SplitMode.SHARES -> {
                            val totalShares = members.sumOf { shares[it.id] ?: 1 }
                            val perShareValue = if (totalShares > 0 && totalAmount > 0) totalAmount / totalShares else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assign Shares / Parts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%.2f", perShareValue)} per share",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            members.forEach { member ->
                                val memberShares = shares[member.id] ?: 1
                                val memberCost = memberShares * perShareValue

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        MemberAvatar(name = member.name, colorHex = member.avatarColorHex, iconName = member.avatarIcon, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "$currencySymbol${String.format(Locale.US, "%.2f", memberCost)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (memberShares > 0) shares[member.id] = memberShares - 1
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        Text(
                                            text = "$memberShares",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { shares[member.id] = memberShares + 1 },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settlement Summary Card
            if (totalAmount > 0) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Settlement Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• $payerName paid the full amount of $currencySymbol${String.format(Locale.US, "%.2f", totalAmount)} upfront.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // List debtors
                        members.filter { it.id != payerId }.forEach { m ->
                            val owed = when (splitMode) {
                                SplitMode.EQUALLY -> {
                                    val count = includedMemberIds.count { it.value }
                                    if ((includedMemberIds[m.id] == true) && count > 0) totalAmount / count else 0.0
                                }
                                SplitMode.EXACT -> exactAmounts[m.id]?.toDoubleOrNull() ?: 0.0
                                SplitMode.PERCENTAGE -> {
                                    val p = percentages[m.id]?.toDoubleOrNull() ?: 0.0
                                    (p / 100.0) * totalAmount
                                }
                                SplitMode.SHARES -> {
                                    val totalSh = members.sumOf { shares[it.id] ?: 1 }
                                    val sh = shares[m.id] ?: 1
                                    if (totalSh > 0) (sh.toDouble() / totalSh) * totalAmount else 0.0
                                }
                            }
                            if (owed > 0.0) {
                                Text(
                                    text = "• ${m.name} owes $payerName $currencySymbol${String.format(Locale.US, "%.2f", owed)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes / Memo
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Split Memo (Optional)") },
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Split Expense Button
            Button(
                onClick = {
                    if (totalAmount <= 0.0) {
                        errorMessage = "Please enter a valid bill amount greater than 0"
                        return@Button
                    }
                    val desc = descriptionText.ifBlank { selectedCategory }

                    // Build summary note for split
                    val splitSummary = buildString {
                        append("Split (${splitMode.label}): Paid by $payerName ($currencySymbol${String.format(Locale.US, "%.2f", totalAmount)}). ")
                        members.forEach { m ->
                            val owed = when (splitMode) {
                                SplitMode.EQUALLY -> {
                                    val count = includedMemberIds.count { it.value }
                                    if ((includedMemberIds[m.id] == true) && count > 0) totalAmount / count else 0.0
                                }
                                SplitMode.EXACT -> exactAmounts[m.id]?.toDoubleOrNull() ?: 0.0
                                SplitMode.PERCENTAGE -> {
                                    val p = percentages[m.id]?.toDoubleOrNull() ?: 0.0
                                    (p / 100.0) * totalAmount
                                }
                                SplitMode.SHARES -> {
                                    val totalSh = members.sumOf { shares[it.id] ?: 1 }
                                    val sh = shares[m.id] ?: 1
                                    if (totalSh > 0) (sh.toDouble() / totalSh) * totalAmount else 0.0
                                }
                            }
                            if (owed > 0.0) {
                                append("${m.name}: $currencySymbol${String.format(Locale.US, "%.2f", owed)}. ")
                            }
                        }
                        if (noteText.isNotBlank()) {
                            append("Note: ${noteText.trim()}")
                        }
                    }

                    val finalSplitType = if (splitMode == SplitMode.EQUALLY) "Split Equally" else "Split Custom"

                    onSave(
                        totalAmount,
                        selectedCategory,
                        desc,
                        payerId,
                        payerName,
                        finalSplitType,
                        selectedPaymentMethod,
                        splitSummary.trim(),
                        datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_split_expense_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Split Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
