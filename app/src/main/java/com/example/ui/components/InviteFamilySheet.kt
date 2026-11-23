package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AuthUser
import com.example.data.model.HouseholdRole
import com.example.data.model.InvitationEntity
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamSuccess
import com.example.util.InvitationTokenUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFamilySheet(
    householdName: String,
    authUser: AuthUser?,
    isAuthLoading: Boolean,
    currentUserRole: HouseholdRole,
    isSubmitting: Boolean,
    errorMessage: String?,
    createdInvitation: InvitationEntity?,
    createdShareLink: String?,
    emailDeliveryConfigured: Boolean,
    onDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    onClearError: () -> Unit,
    onSendEmailInvite: (email: String, memberName: String, personalMessage: String, role: HouseholdRole) -> Unit,
    onGenerateShareLink: (role: HouseholdRole, personalMessage: String) -> Unit,
    onInviteAnother: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Email, 1 = Share Link
    var memberName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var personalMessage by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(HouseholdRole.MEMBER) }
    var localEmailError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("invite_family_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.invite_sheet_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.invite_sheet_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("invite_sheet_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auth gate: Real multi-device invitations require Google Sign-In
            if (authUser == null) {
                SignInRequiredCard(
                    isLoading = isAuthLoading,
                    onSignInClick = onSignInClick
                )
                return@Column
            }

            // RBAC gate: Only OWNER or ADMIN can invite
            if (!currentUserRole.canInviteMembers()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Only household Owners and Admins can invite new members. Ask your household Owner for access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                return@Column
            }

            // Show confirmed success view ONLY after backend confirms invitation creation
            if (createdInvitation != null && !createdShareLink.isNullOrBlank()) {
                InvitationSuccessView(
                    invitation = createdInvitation,
                    shareLink = createdShareLink,
                    emailDeliveryConfigured = emailDeliveryConfigured,
                    onCopyLink = {
                        copyLinkToClipboard(context, createdShareLink)
                    },
                    onShareWhatsApp = {
                        shareViaWhatsApp(
                            context = context,
                            householdName = createdInvitation.householdName,
                            inviterName = createdInvitation.inviterName,
                            inviteLink = createdShareLink,
                            personalMessage = createdInvitation.personalMessage
                        )
                    },
                    onNativeShare = {
                        shareViaNativeSheet(
                            context = context,
                            householdName = createdInvitation.householdName,
                            inviterName = createdInvitation.inviterName,
                            inviteLink = createdShareLink,
                            personalMessage = createdInvitation.personalMessage
                        )
                    },
                    onInviteAnother = {
                        memberName = ""
                        email = ""
                        personalMessage = ""
                        localEmailError = null
                        onInviteAnother()
                    },
                    onDone = onDismiss
                )
                return@Column
            }

            // Invitation Method Segmented Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InviteMethodTab(
                        selected = selectedTab == 0,
                        icon = Icons.Default.Email,
                        label = stringResource(R.string.invite_method_email),
                        onClick = {
                            selectedTab = 0
                            localEmailError = null
                            onClearError()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invite_method_email_tab")
                    )
                    InviteMethodTab(
                        selected = selectedTab == 1,
                        icon = Icons.Default.Link,
                        label = stringResource(R.string.invite_method_link),
                        onClick = {
                            selectedTab = 1
                            localEmailError = null
                            onClearError()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invite_method_link_tab")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Role selector (OWNER can invite Member or Admin; ADMIN can invite Member)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Role in ${householdName.ifBlank { "My Household" }}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == HouseholdRole.MEMBER,
                        onClick = { selectedRole = HouseholdRole.MEMBER },
                        label = { Text("Member") },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("invite_role_member_chip")
                    )
                    if (currentUserRole == HouseholdRole.OWNER) {
                        FilterChip(
                            selected = selectedRole == HouseholdRole.ADMIN,
                            onClick = { selectedRole = HouseholdRole.ADMIN },
                            label = { Text("Admin") },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("invite_role_admin_chip")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error Banner (Backend or validation error)
            val displayedError = localEmailError ?: errorMessage
            AnimatedVisibility(visible = !displayedError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("invite_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayedError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "invite_method_content"
            ) { tab ->
                if (tab == 0) {
                    // Method A: Invite via Email
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = memberName,
                            onValueChange = { memberName = it },
                            label = { Text("Member's name (optional)") },
                            placeholder = { Text("e.g. Priya") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_member_name_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localEmailError = null
                                onClearError()
                            },
                            label = { Text("Email address *") },
                            placeholder = { Text("name@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            isError = localEmailError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_email_input")
                        )

                        OutlinedTextField(
                            value = personalMessage,
                            onValueChange = { if (it.length <= 300) personalMessage = it },
                            label = { Text("Personal message (optional)") },
                            placeholder = { Text("Join our family budget on FamSpend!") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_message_input")
                        )

                        Button(
                            onClick = {
                                val trimmedEmail = email.trim()
                                when {
                                    trimmedEmail.isEmpty() -> {
                                        localEmailError = "Please enter an email address."
                                    }
                                    !InvitationTokenUtil.isValidEmail(trimmedEmail) -> {
                                        localEmailError = "Please enter a valid email address."
                                    }
                                    else -> {
                                        localEmailError = null
                                        onSendEmailInvite(
                                            trimmedEmail,
                                            memberName.trim(),
                                            personalMessage.trim(),
                                            selectedRole
                                        )
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("invite_send_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Creating Invitation...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.invite_send_button),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Method B: Share Invitation Link
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Generate a unique, single-use secure invitation link valid for 7 days. Share it via WhatsApp, SMS, or copy it directly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        OutlinedTextField(
                            value = personalMessage,
                            onValueChange = { if (it.length <= 300) personalMessage = it },
                            label = { Text("Personal note (optional)") },
                            placeholder = { Text("Let's track our monthly household expenses together!") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_link_message_input")
                        )

                        Button(
                            onClick = {
                                onGenerateShareLink(selectedRole, personalMessage.trim())
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("invite_generate_link_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Generating Secure Link...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.invite_generate_link_button),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteMethodTab(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SignInRequiredCard(
    isLoading: Boolean,
    onSignInClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invite_sign_in_prompt_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Sign in to Invite Family",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.invite_sign_in_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("invite_sign_in_google_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing in...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InvitationSuccessView(
    invitation: InvitationEntity,
    shareLink: String,
    emailDeliveryConfigured: Boolean,
    onCopyLink: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onNativeShare: () -> Unit,
    onInviteAnother: () -> Unit,
    onDone: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val expiresText = dateFormatter.format(Date(invitation.expiresAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invite_success_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(FamSuccess.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = FamSuccess,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.invite_success_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        val targetDesc = when {
            invitation.inviteeName.isNotBlank() && invitation.inviteeEmail.isNotBlank() ->
                "Ready for ${invitation.inviteeName} (${invitation.inviteeEmail}) • Expires $expiresText"
            invitation.inviteeEmail.isNotBlank() ->
                "Ready for ${invitation.inviteeEmail} • Expires $expiresText"
            else ->
                "Single-use link for ${invitation.householdName} • Expires $expiresText"
        }
        Text(
            text = targetDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Honest communication if no transactional email delivery provider is configured
        if (invitation.inviteeEmail.isNotBlank() && !emailDeliveryConfigured) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.invite_email_delivery_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Link preview card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = shareLink,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Copy + WhatsApp primary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCopyLink,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("invite_copy_link_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.invite_copy_link),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Button(
                onClick = onShareWhatsApp,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("invite_whatsapp_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Native Android share button
        Button(
            onClick = onNativeShare,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("invite_native_share_button")
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.invite_share_more),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onInviteAnother) {
                Text("Invite another member", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDone) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun copyLinkToClipboard(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("FamSpend Invitation Link", link))
    Toast.makeText(context, "Invitation link copied!", Toast.LENGTH_SHORT).show()
}

fun shareViaWhatsApp(
    context: Context,
    householdName: String,
    inviterName: String,
    inviteLink: String,
    personalMessage: String = ""
) {
    val shareBody = InvitationTokenUtil.buildShareMessage(
        householdName = householdName,
        inviterName = inviterName,
        inviteLink = inviteLink,
        personalMessage = personalMessage
    )
    try {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, shareBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(whatsappIntent)
    } catch (_: Exception) {
        // Fallback 1: WhatsApp web deep link, Fallback 2: Android native share chooser
        try {
            val encoded = Uri.encode(shareBody)
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
            shareViaNativeSheet(context, householdName, inviterName, inviteLink, personalMessage)
        }
    }
}

fun shareViaNativeSheet(
    context: Context,
    householdName: String,
    inviterName: String,
    inviteLink: String,
    personalMessage: String = ""
) {
    val shareBody = InvitationTokenUtil.buildShareMessage(
        householdName = householdName,
        inviterName = inviterName,
        inviteLink = inviteLink,
        personalMessage = personalMessage
    )
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join $householdName on FamSpend")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(sendIntent, "Share FamSpend Invitation").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        copyLinkToClipboard(context, inviteLink)
    }
}
