package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.HouseholdRole
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryGradientStart
import com.example.ui.theme.FamSuccess
import com.example.viewmodel.InvitationLandingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvitationAcceptanceDialog(
    state: InvitationLandingState,
    isAuthLoading: Boolean,
    onSignInClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state is InvitationLandingState.Hidden) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("invitation_landing_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // FamSpend Branding Header
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(FamPrimaryGradientStart, FamPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "FamSpend",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "FamSpend",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Family Household Invitation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    when (state) {
                        InvitationLandingState.Hidden -> Unit

                        InvitationLandingState.Loading -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = FamPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Verifying your invitation...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Checking security token and expiration.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is InvitationLandingState.RequiresSignIn -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invitation_requires_sign_in_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Sign in to view your invitation",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sign in with Google to verify your identity and join the household. Your invitation link is saved and will open automatically right after sign-in.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = onSignInClick,
                                        enabled = !isAuthLoading,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("invitation_sign_in_continue_btn")
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Signing in...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = onDeclineClick,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.invite_decline_button))
                                    }
                                }
                            }
                        }

                        is InvitationLandingState.Ready -> {
                            val inv = state.invitation
                            val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                            val expiresStr = dateFmt.format(Date(inv.expiresAt))
                            val assignedRole = HouseholdRole.fromString(inv.role)

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invitation_ready_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${inv.inviterName} invited you to join",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = inv.householdName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    RoleBadgePill(role = assignedRole)

                                    if (inv.personalMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "\"${inv.personalMessage}\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Manage expenses, budgets, and shared household goals together. Valid until $expiresStr.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    if (!state.errorMessage.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                            border = BorderStroke(1.dp, FamDanger.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = state.errorMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FamDanger,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(22.dp))

                                    Button(
                                        onClick = onAcceptClick,
                                        enabled = !state.isAccepting,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("accept_invitation_btn")
                                    ) {
                                        if (state.isAccepting) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Joining Household...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(
                                                text = stringResource(R.string.invite_accept_button),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = onDeclineClick,
                                        enabled = !state.isAccepting,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("decline_invitation_btn")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.invite_decline_button),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        is InvitationLandingState.AcceptedSuccess -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invitation_accepted_success_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(FamSuccess.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = FamSuccess,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = stringResource(R.string.invite_welcome_family),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "You have joined ${state.householdName} as ${state.role.displayLabel}. Your shared household expenses and budgets are syncing now.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Button(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("invitation_open_household_btn")
                                    ) {
                                        Text("Open Household", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        is InvitationLandingState.Error -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invitation_error_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(FamDanger.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = FamDanger,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = state.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Button(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FamPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("invitation_error_close_btn")
                                    ) {
                                        Text("Back to FamSpend", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
