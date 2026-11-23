package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AuthUser
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.InvitationEntity
import com.example.data.model.InvitationStatus
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamSuccess
import com.example.ui.theme.FamWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberManagementSheet(
    householdName: String,
    authUser: AuthUser?,
    currentUserRole: HouseholdRole,
    members: List<FamilyMemberEntity>,
    activeMemberId: String?,
    invitations: List<InvitationEntity>,
    isInvitationsLoading: Boolean,
    feedbackMessage: String?,
    getCachedLink: (String) -> String?,
    onDismiss: () -> Unit,
    onClearFeedback: () -> Unit,
    onAddNewMemberClick: () -> Unit,
    onAcceptInviteLinkClick: () -> Unit,
    onSwitchActiveMember: (String) -> Unit,
    onEditMember: (FamilyMemberEntity) -> Unit,
    onChangeMemberRole: (FamilyMemberEntity, HouseholdRole) -> Unit,
    onRemoveMember: (FamilyMemberEntity) -> Unit,
    onResendInvitation: (invitationId: String, onLinkReady: (String) -> Unit) -> Unit,
    onRevokeInvitation: (invitationId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var memberPendingRemoval by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var invitationPendingRevoke by remember { mutableStateOf<InvitationEntity?>(null) }

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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("family_member_management_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.member_management_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = householdName.ifBlank { "My Household" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RoleBadgePill(role = currentUserRole)
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("member_mgmt_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action feedback banner
            AnimatedVisibility(visible = !feedbackMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedbackMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearFeedback,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // SECTION C (Top quick entry): Add New Member — Unified Invitation Flow
            // =================================================================
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_mgmt_add_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.member_management_add_new_member),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Invite by email or share a secure single-use invitation link.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddNewMemberClick,
                            enabled = currentUserRole.canInviteMembers(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("member_mgmt_add_new_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invite Member", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onAcceptInviteLinkClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("member_mgmt_join_link_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Join Link", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // SECTION A: Current Family Members
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.member_management_current_members),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${members.size} active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_mgmt_current_section")
            ) {
                members.forEach { member ->
                    val memberRole = member.parsedHouseholdRole()
                    val isSelf = authUser != null && (member.userId == authUser.uid || member.id == authUser.uid)
                    val isActive = member.id == activeMemberId
                    val canPromoteToAdmin = currentUserRole.canChangeRole(memberRole, HouseholdRole.ADMIN, isSelf)
                    val canDemoteToMember = currentUserRole.canChangeRole(memberRole, HouseholdRole.MEMBER, isSelf)
                    val canRemove = currentUserRole.canRemoveMember(memberRole, isSelf)
                    var menuExpanded by remember(member.id) { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_row_${member.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(
                                name = member.name,
                                colorHex = member.avatarColorHex,
                                iconName = member.avatarIcon,
                                size = 44.dp,
                                isActive = isActive
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RoleBadgePill(role = memberRole)
                                }
                                if (member.email.isNotBlank()) {
                                    Text(
                                        text = member.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = member.parsedMembershipStatus().displayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FamSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isActive) {
                                        Text(
                                            text = "• Active on device",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("member_actions_${member.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Manage ${member.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (!isActive) {
                                        DropdownMenuItem(
                                            text = { Text("Switch Active Profile") },
                                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onSwitchActiveMember(member.id)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Edit Profile") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditMember(member)
                                        }
                                    )
                                    if (canPromoteToAdmin && memberRole != HouseholdRole.ADMIN) {
                                        DropdownMenuItem(
                                            text = { Text("Make Admin") },
                                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onChangeMemberRole(member, HouseholdRole.ADMIN)
                                            }
                                        )
                                    }
                                    if (canDemoteToMember && memberRole != HouseholdRole.MEMBER) {
                                        DropdownMenuItem(
                                            text = { Text("Set as Member") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onChangeMemberRole(member, HouseholdRole.MEMBER)
                                            }
                                        )
                                    }
                                    if (canRemove) {
                                        DropdownMenuItem(
                                            text = { Text("Remove Member", color = FamDanger) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = FamDanger
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                memberPendingRemoval = member
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(18.dp))

            // =================================================================
            // SECTION B: Pending Invitations
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.member_management_pending_invitations),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val pendingCount = invitations.count { it.effectiveStatus() == InvitationStatus.PENDING }
                Text(
                    text = "$pendingCount pending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_mgmt_pending_section")
            ) {
                if (isInvitationsLoading) {
                    repeat(2) {
                        InvitationSkeletonCard()
                    }
                } else if (invitations.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.invite_empty_pending),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sent email and link invitations will appear here with their live status.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    invitations.forEach { invitation ->
                        InvitationItemCard(
                            invitation = invitation,
                            canManage = currentUserRole.canManageInvitations(),
                            onCopyLink = {
                                val cached = getCachedLink(invitation.id)
                                if (cached != null) {
                                    copyLinkToClipboard(context, cached)
                                } else {
                                    // Raw tokens are never stored on disk; regenerate a fresh link securely
                                    onResendInvitation(invitation.id) { freshLink ->
                                        copyLinkToClipboard(context, freshLink)
                                    }
                                }
                            },
                            onResend = {
                                onResendInvitation(invitation.id) { freshLink ->
                                    shareViaNativeSheet(
                                        context = context,
                                        householdName = invitation.householdName,
                                        inviterName = invitation.inviterName,
                                        inviteLink = freshLink,
                                        personalMessage = invitation.personalMessage
                                    )
                                }
                            },
                            onRevoke = {
                                invitationPendingRevoke = invitation
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirm Remove Member Dialog
    memberPendingRemoval?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text("Remove ${member.name}?") },
            text = {
                Text("They will lose access to shared household expenses and budgets. Existing historical expense records will be preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingRemoval = null
                        onRemoveMember(member)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamDanger)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Revoke Invitation Dialog
    invitationPendingRevoke?.let { inv ->
        AlertDialog(
            onDismissRequest = { invitationPendingRevoke = null },
            title = { Text("Revoke Invitation?") },
            text = {
                val target = inv.inviteeEmail.ifBlank { inv.inviteeName.ifBlank { "this invitation link" } }
                Text("Revoking $target immediately invalidates the link so it can no longer be accepted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        invitationPendingRevoke = null
                        onRevokeInvitation(inv.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamDanger),
                    modifier = Modifier.testTag("confirm_revoke_invitation_btn")
                ) {
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { invitationPendingRevoke = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InvitationItemCard(
    invitation: InvitationEntity,
    canManage: Boolean,
    onCopyLink: () -> Unit,
    onResend: () -> Unit,
    onRevoke: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val status = invitation.effectiveStatus()
    val invitedDate = dateFormat.format(Date(invitation.createdAt))
    val expiresDate = dateFormat.format(Date(invitation.expiresAt))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invitation_card_${invitation.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (invitation.isLinkOnlyInvitation()) Icons.Default.Link else Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = invitation.inviteeName.ifBlank {
                                if (invitation.isLinkOnlyInvitation()) "Shareable Invitation Link" else invitation.inviteeEmail
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (invitation.inviteeEmail.isNotBlank()) {
                            Text(
                                text = invitation.inviteeEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                InvitationStatusPill(status = status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Invited $invitedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expires $expiresDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canManage && status != InvitationStatus.ACCEPTED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status == InvitationStatus.PENDING) {
                        OutlinedButton(
                            onClick = onCopyLink,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("copy_invite_btn_${invitation.id}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onResend,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("resend_invite_btn_${invitation.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    if (status == InvitationStatus.PENDING) {
                        OutlinedButton(
                            onClick = onRevoke,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FamDanger.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FamDanger),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("revoke_invite_btn_${invitation.id}")
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revoke", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleBadgePill(role: HouseholdRole) {
    val (bg, fg) = when (role) {
        HouseholdRole.OWNER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        HouseholdRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        HouseholdRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = role.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun InvitationStatusPill(status: InvitationStatus) {
    val (bg, fg) = when (status) {
        InvitationStatus.PENDING -> FamWarning.copy(alpha = 0.16f) to FamWarning
        InvitationStatus.ACCEPTED -> FamSuccess.copy(alpha = 0.16f) to FamSuccess
        InvitationStatus.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        InvitationStatus.REVOKED -> FamDanger.copy(alpha = 0.15f) to FamDanger
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = status.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun InvitationSkeletonCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )
            }
        }
    }
}
