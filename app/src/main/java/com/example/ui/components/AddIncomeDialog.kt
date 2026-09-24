package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.IncomeCategory
import com.example.data.model.IncomeFrequency
import com.example.data.model.IncomePaymentMethod
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val IncomeGreen = Color(0xFF22C55E)
val IncomeGreenLight = Color(0xFFDCFCE7)
val IncomeGreenDark = Color(0xFF16A34A)

fun formatIndianNumber(value: String): String {
    val clean = value.filter { it.isDigit() || it == '.' }
    if (clean.isEmpty()) return ""
    val parts = clean.split(".", limit = 2)
    val intPart = parts[0]
    if (intPart.isEmpty()) return if (parts.size > 1) "0.${parts[1]}" else ""

    val n = intPart.length
    if (n <= 3) {
        return if (parts.size > 1) "$intPart.${parts[1]}" else intPart
    }
    val last3 = intPart.substring(n - 3)
    val rest = intPart.substring(0, n - 3)
    val sb = StringBuilder()
    var count = 0
    for (i in rest.length - 1 downTo 0) {
        sb.append(rest[i])
        count++
        if (count == 2 && i != 0) {
            sb.append(',')
            count = 0
        }
    }
    val formattedRest = sb.reverse().toString()
    val formattedInt = "$formattedRest,$last3"
    return if (parts.size > 1) "$formattedInt.${parts[1]}" else formattedInt
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    members: List<FamilyMemberEntity>,
    activeMember: FamilyMemberEntity?,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        source: String,
        category: String,
        date: Long,
        receivedBy: String,
        receivedByName: String,
        paymentMethod: String,
        isRecurring: Boolean,
        recurringFrequency: String?,
        recurringStartDate: Long?,
        note: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var rawAmount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(IncomeCategory.SALARY) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedMemberId by remember {
        mutableStateOf(activeMember?.id ?: members.firstOrNull()?.id ?: "")
    }

    var selectedPaymentMethod by remember { mutableStateOf(IncomePaymentMethod.BANK_TRANSFER) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(IncomeFrequency.MONTHLY) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var recurringStartDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRecurringDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    // Validation and shake animation
    var validationError by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val cleanNumericValue = rawAmount.replace(",", "")
    val numericAmount = cleanNumericValue.toDoubleOrNull() ?: 0.0
    val isFormValid = numericAmount > 0.0 && source.trim().isNotBlank()

    // Request auto-focus on amount
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Date picker state - disallow future dates
    val todayMillis = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayMillis
            }
        }
    )

    val recurringDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = recurringStartDateMillis
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        modifier = Modifier.testTag("add_income_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Title "Log Income", green icon, close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IncomeGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Log Income",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("add_income_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIELD 1 — AMOUNT (Centered, 48sp, Green color, ₹ symbol, Indian format)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currencySymbol,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = rawAmount,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            // Prevent multiple decimal points
                            if (filtered.count { it == '.' } <= 1) {
                                rawAmount = formatIndianNumber(filtered)
                                if (validationError != null && rawAmount.isNotBlank()) {
                                    validationError = null
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "0.00",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                textAlign = TextAlign.Start
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                            textAlign = TextAlign.Start
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .testTag("income_amount_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIELD 2 — INCOME SOURCE / TITLE
            Text(
                text = "Income Source",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = source,
                onValueChange = {
                    if (it.length <= 100) {
                        source = it
                        if (validationError != null && it.isNotBlank()) {
                            validationError = null
                        }
                    }
                },
                placeholder = { Text("e.g. Monthly Salary, Freelance Payment") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IncomeGreen,
                    focusedLabelColor = IncomeGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_source_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 3 — INCOME CATEGORY (Horizontal scrollable chips)
            Text(
                text = "Income Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(IncomeCategory.entries) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = cat }
                            .testTag("income_cat_${cat.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = cat.iconEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 4 — DATE RECEIVED (No future dates)
            Text(
                text = "Date Received",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .testTag("income_date_picker_trigger")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = remember(selectedDateMillis) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 5 — RECEIVED BY
            Text(
                text = "Received By",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(members) { member ->
                    val isSelected = selectedMemberId == member.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) IncomeGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedMemberId = member.id }
                            .testTag("income_member_${member.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            MemberAvatar(
                                name = member.name,
                                colorHex = member.avatarColorHex,
                                iconName = member.avatarIcon,
                                size = 28.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) IncomeGreenDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 6 — PAYMENT METHOD (Received Via)
            Text(
                text = "Received Via",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(IncomePaymentMethod.entries) { method ->
                    val isSelected = selectedPaymentMethod == method
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) IncomeGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedPaymentMethod = method }
                            .testTag("income_method_${method.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(text = method.iconEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = method.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) IncomeGreenDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 7 — RECURRING INCOME
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Recurring Income",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-logs this income every month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IncomeGreen
                            ),
                            modifier = Modifier.testTag("income_recurring_switch")
                        )
                    }

                    AnimatedVisibility(visible = isRecurring) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Frequency Dropdown
                            ExposedDropdownMenuBox(
                                expanded = frequencyExpanded,
                                onExpandedChange = { frequencyExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedFrequency.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frequency") },
                                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IncomeGreen,
                                        focusedLabelColor = IncomeGreen
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = frequencyExpanded,
                                    onDismissRequest = { frequencyExpanded = false }
                                ) {
                                    IncomeFrequency.entries.forEach { freq ->
                                        DropdownMenuItem(
                                            text = { Text(freq.displayName) },
                                            onClick = {
                                                selectedFrequency = freq
                                                frequencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Start Date
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRecurringDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val startFormatted = remember(recurringStartDateMillis) {
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(recurringStartDateMillis))
                                    }
                                    Column {
                                        Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(startFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Icon(Icons.Default.CalendarToday, null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FIELD 8 — NOTE (Optional)
            Text(
                text = "Note (Optional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Any additional details...") },
                maxLines = 3,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IncomeGreen,
                    focusedLabelColor = IncomeGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_note_input")
            )

            // Inline error message if attempted submit while disabled
            AnimatedVisibility(visible = validationError != null) {
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SAVE BUTTON with shake animation if tapped while invalid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(bottom = 28.dp)
            ) {
                Button(
                    onClick = {
                        if (!isFormValid) {
                            validationError = "Please fill in amount and source"
                            coroutineScope.launch {
                                repeat(3) {
                                    shakeOffset.animateTo(16f, tween(50))
                                    shakeOffset.animateTo(-16f, tween(50))
                                }
                                shakeOffset.animateTo(0f, tween(50))
                            }
                        } else {
                            val memberName = members.find { it.id == selectedMemberId }?.name ?: "Family Member"
                            onSave(
                                numericAmount,
                                source.trim(),
                                selectedCategory.name,
                                selectedDateMillis,
                                selectedMemberId,
                                memberName,
                                selectedPaymentMethod.name,
                                isRecurring,
                                if (isRecurring) selectedFrequency.name else null,
                                if (isRecurring) recurringStartDateMillis else null,
                                note.trim().ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("log_income_submit_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) IncomeGreen else Color(0xFF9CA3AF),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Log Income",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Recurring start date picker dialog
    if (showRecurringDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showRecurringDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        recurringDatePickerState.selectedDateMillis?.let { recurringStartDateMillis = it }
                        showRecurringDatePicker = false
                    }
                ) {
                    Text("OK", color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = recurringDatePickerState)
        }
    }
}
