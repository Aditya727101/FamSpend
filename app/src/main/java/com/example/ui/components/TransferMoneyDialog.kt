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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransferMoneyDialog(
    members: List<FamilyMemberEntity>,
    activeMember: FamilyMemberEntity?,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        fromAccount: String,
        toAccount: String,
        amount: Double,
        date: Long,
        memberId: String,
        memberName: String,
        note: String,
        fee: Double
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultAccounts = listOf(
        "Checking Account",
        "Savings Account",
        "Cash in Hand",
        "Credit Card",
        "Google Pay / UPI",
        "Emergency Fund",
        "Investment Account"
    )

    var fromAccount by remember { mutableStateOf("Checking Account") }
    var toAccount by remember { mutableStateOf("Savings Account") }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var showFeeField by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val defaultMember = activeMember ?: members.firstOrNull()
    var selectedMemberId by remember { mutableStateOf(defaultMember?.id ?: "mem_1") }
    var selectedMemberName by remember { mutableStateOf(defaultMember?.name ?: "Family Member") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("transfer_money_bottom_sheet")
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
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Transfer Between Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Move money between banks, cards, or cash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_transfer_dialog")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick transfer preset templates
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presets = listOf(
                    Triple("Checking ➔ Savings", "Checking Account", "Savings Account"),
                    Triple("ATM Cash", "Checking Account", "Cash in Hand"),
                    Triple("Pay Credit Card", "Checking Account", "Credit Card"),
                    Triple("Load UPI", "Checking Account", "Google Pay / UPI")
                )
                presets.forEach { (label, from, to) ->
                    val isMatch = fromAccount == from && toAccount == to
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMatch) Color(0xFFEDE9FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isMatch) BorderStroke(1.5.dp, Color(0xFF7C3AED)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                fromAccount = from
                                toAccount = to
                                errorMessage = null
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMatch) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transfer Accounts Selector (From -> To with interactive swap button)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // FROM Account
                    Text(
                        text = "FROM ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountSelectorDropdown(
                        selectedAccount = fromAccount,
                        accounts = defaultAccounts,
                        onAccountSelected = {
                            fromAccount = it
                            errorMessage = null
                        },
                        tag = "transfer_from_dropdown"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Swap Button Row
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val temp = fromAccount
                                    fromAccount = toAccount
                                    toAccount = temp
                                    errorMessage = null
                                }
                                .testTag("transfer_swap_accounts_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Accounts",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // TO Account
                    Text(
                        text = "TO ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountSelectorDropdown(
                        selectedAccount = toAccount,
                        accounts = defaultAccounts,
                        onAccountSelected = {
                            toAccount = it
                            errorMessage = null
                        },
                        tag = "transfer_to_dropdown"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Transfer Amount ($currencySymbol)") },
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
                    .testTag("transfer_amount_input"),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Amount Increment Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val increments = listOf(500, 1000, 2000, 5000)
                increments.forEach { inc ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                val updated = current + inc
                                amountText = if (updated % 1.0 == 0.0) updated.toInt().toString() else String.format(Locale.US, "%.2f", updated)
                                errorMessage = null
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "+$currencySymbol$inc",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                        Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis())
                    ),
                    onValueChange = {},
                    label = { Text("Transfer Date") },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                )
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Initiated By Member
            Text(
                text = "Initiated By",
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
                    val isSelected = member.id == selectedMemberId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                selectedMemberId = member.id
                                selectedMemberName = member.name
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

            // Note / Reference
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Transfer Reference / Note (Optional)") },
                placeholder = { Text("e.g. Monthly savings, Card bill payment") },
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Transfer Fee Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFeeField = !showFeeField },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Transfer Fee or Charges?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = showFeeField,
                    onCheckedChange = { showFeeField = it }
                )
            }

            AnimatedVisibility(visible = showFeeField) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("Transfer Fee ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid transfer amount greater than 0"
                        return@Button
                    }
                    if (fromAccount.equals(toAccount, ignoreCase = true)) {
                        errorMessage = "Source and destination accounts cannot be the same"
                        return@Button
                    }
                    val fee = feeText.toDoubleOrNull() ?: 0.0
                    val finalDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    onSave(
                        fromAccount,
                        toAccount,
                        amt,
                        finalDate,
                        selectedMemberId,
                        selectedMemberName,
                        noteText,
                        fee
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transfer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Record Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectorDropdown(
    selectedAccount: String,
    accounts: List<String>,
    onAccountSelected: (String) -> Unit,
    tag: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        OutlinedTextField(
            value = selectedAccount,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                val icon = when {
                    selectedAccount.contains("Checking", true) -> Icons.Default.AccountBalance
                    selectedAccount.contains("Savings", true) -> Icons.Default.Savings
                    selectedAccount.contains("Cash", true) -> Icons.Default.Money
                    selectedAccount.contains("Card", true) -> Icons.Default.CreditCard
                    selectedAccount.contains("UPI", true) || selectedAccount.contains("Pay", true) -> Icons.Default.Smartphone
                    else -> Icons.Default.AccountBalanceWallet
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account, fontWeight = if (account == selectedAccount) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }
    }
}
