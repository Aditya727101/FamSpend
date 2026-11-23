package com.example.data.repository

import com.example.data.AuthUser
import com.example.data.dao.FamilyMemberDao
import com.example.data.dao.HouseholdDao
import com.example.data.dao.InvitationDao
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.InvitationEntity
import com.example.data.model.InvitationStatus
import com.example.data.model.MembershipStatus
import com.example.data.sync.FirestoreErrorHandler
import com.example.data.sync.OperationType
import com.example.util.InvitationTokenUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AcceptedHouseholdResult(
    val householdId: String,
    val householdName: String,
    val assignedRole: HouseholdRole,
    val invitation: InvitationEntity
)

sealed class InvitationCreateResult {
    data class Success(
        val invitation: InvitationEntity,
        val shareLink: String,
        val emailDeliveryConfigured: Boolean = false
    ) : InvitationCreateResult()

    data class Error(val message: String) : InvitationCreateResult()
}

sealed class InvitationActionResult {
    data class Success(
        val message: String,
        val updatedShareLink: String? = null
    ) : InvitationActionResult()

    data class Error(val message: String) : InvitationActionResult()
}

sealed class InvitationValidationResult {
    data class Valid(
        val invitation: InvitationEntity,
        val rawToken: String
    ) : InvitationValidationResult()

    data class RequiresSignIn(val rawToken: String) : InvitationValidationResult()
    data class Expired(val message: String, val invitation: InvitationEntity? = null) : InvitationValidationResult()
    data class Revoked(val message: String) : InvitationValidationResult()
    data class AlreadyUsed(val message: String) : InvitationValidationResult()
    data class Invalid(val message: String) : InvitationValidationResult()
}

sealed class InvitationAcceptResult {
    data class Accepted(
        val householdId: String,
        val householdName: String,
        val role: HouseholdRole
    ) : InvitationAcceptResult()

    data object RequiresSignIn : InvitationAcceptResult()
    data class EmailMismatch(val expectedEmail: String, val actualEmail: String) : InvitationAcceptResult()
    data class AlreadyMember(val message: String) : InvitationAcceptResult()
    data class Error(val message: String) : InvitationAcceptResult()
}

sealed class MemberManagementResult {
    data class Success(val message: String) : MemberManagementResult()
    data class Error(val message: String) : MemberManagementResult()
}

class InvitationRuleException(
    val code: String,
    override val message: String
) : Exception(message)

interface InvitationCloudBackend {
    suspend fun resolveOrBootstrapHousehold(
        localHousehold: HouseholdEntity,
        ownerUser: AuthUser
    ): Result<HouseholdEntity>

    suspend fun createInvitation(
        invitation: InvitationEntity,
        adminUids: List<String>
    ): Result<Unit>

    suspend fun extendInvitationExpiration(
        tokenHash: String,
        newExpiresAtMillis: Long
    ): Result<Unit>

    suspend fun revokeInvitation(
        tokenHash: String,
        revokedAtMillis: Long
    ): Result<Unit>

    suspend fun fetchInvitationByTokenHash(tokenHash: String): Result<InvitationEntity?>

    suspend fun acceptInvitationTransaction(
        tokenHash: String,
        acceptingUser: AuthUser
    ): Result<AcceptedHouseholdResult>

    suspend fun updateMemberRole(
        householdId: String,
        memberId: String,
        newRole: HouseholdRole
    ): Result<Unit>

    suspend fun removeMember(
        householdId: String,
        memberId: String
    ): Result<Unit>
}

class FirestoreInvitationCloudBackend(
    private val firestoreProvider: () -> FirebaseFirestore?
) : InvitationCloudBackend {

    private fun requireDb(): FirebaseFirestore {
        return firestoreProvider()
            ?: throw IllegalStateException("Cloud database is currently unavailable. Please check your connection.")
    }

    override suspend fun resolveOrBootstrapHousehold(
        localHousehold: HouseholdEntity,
        ownerUser: AuthUser
    ): Result<HouseholdEntity> {
        return try {
            val db = requireDb()
            val userRef = db.collection("users").document(ownerUser.uid)
            val userSnap = userRef.get().await()
            val existingActiveHouseholdId = userSnap.getString("activeHouseholdId")?.takeIf { it.isNotBlank() }

            if (existingActiveHouseholdId != null) {
                val memberSnap = db.collection("households")
                    .document(existingActiveHouseholdId)
                    .collection("members")
                    .document(ownerUser.uid)
                    .get()
                    .await()
                if (memberSnap.exists() && memberSnap.getString("membershipStatus") == MembershipStatus.ACTIVE.value) {
                    val householdSnap = db.collection("households")
                        .document(existingActiveHouseholdId)
                        .get()
                        .await()
                    if (householdSnap.exists()) {
                        return Result.success(
                            HouseholdEntity(
                                householdId = householdSnap.id,
                                householdName = householdSnap.getString("householdName") ?: localHousehold.householdName,
                                inviteCode = householdSnap.getString("inviteCode") ?: householdSnap.id,
                                defaultCurrency = householdSnap.getString("defaultCurrency") ?: localHousehold.defaultCurrency,
                                totalMonthlyBudget = householdSnap.getDouble("totalMonthlyBudget") ?: localHousehold.totalMonthlyBudget,
                                lastSyncedTimestamp = System.currentTimeMillis(),
                                isLiveSyncEnabled = householdSnap.getBoolean("isLiveSyncEnabled") ?: true,
                                ownerUserId = householdSnap.getString("ownerUserId") ?: ownerUser.uid
                            )
                        )
                    }
                }
            }

            // Allocate a collision-free household ID per owner on initial cloud bootstrap
            val targetHouseholdId = if (localHousehold.householdId == "FAM-1001" || localHousehold.householdId.isBlank()) {
                "FAM-${ownerUser.uid.take(8).uppercase()}"
            } else {
                localHousehold.householdId
            }

            val householdRef = db.collection("households").document(targetHouseholdId)
            val ownerMemberRef = householdRef.collection("members").document(ownerUser.uid)
            val now = System.currentTimeMillis()
            val cleanName = localHousehold.householdName.ifBlank { "${ownerUser.displayName}'s Household" }

            db.runTransaction { tx ->
                val hSnap = tx.get(householdRef)
                if (!hSnap.exists()) {
                    val householdPayload = hashMapOf<String, Any>(
                        "householdId" to targetHouseholdId,
                        "householdName" to cleanName,
                        "inviteCode" to targetHouseholdId,
                        "defaultCurrency" to localHousehold.defaultCurrency.ifBlank { "₹" },
                        "totalMonthlyBudget" to localHousehold.totalMonthlyBudget.coerceAtLeast(0.0),
                        "lastSyncedTimestamp" to now,
                        "isLiveSyncEnabled" to true,
                        "ownerUserId" to ownerUser.uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val ownerMemberPayload = hashMapOf<String, Any>(
                        "id" to ownerUser.uid,
                        "userId" to ownerUser.uid,
                        "name" to ownerUser.displayName.ifBlank { "Owner" },
                        "email" to InvitationTokenUtil.normalizeEmail(ownerUser.email),
                        "role" to "Primary",
                        "householdRole" to HouseholdRole.OWNER.value,
                        "membershipStatus" to MembershipStatus.ACTIVE.value,
                        "avatarColorHex" to "#4F46E5",
                        "avatarIcon" to "person",
                        "householdId" to targetHouseholdId,
                        "monthlyContributionGoal" to 0.0,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(householdRef, householdPayload)
                    tx.set(ownerMemberRef, ownerMemberPayload)
                }

                val userPayload = hashMapOf<String, Any>(
                    "uid" to ownerUser.uid,
                    "displayName" to ownerUser.displayName.ifBlank { "User" },
                    "email" to InvitationTokenUtil.normalizeEmail(ownerUser.email),
                    "activeHouseholdId" to targetHouseholdId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(userRef, userPayload)
            }.await()

            Result.success(
                localHousehold.copy(
                    householdId = targetHouseholdId,
                    householdName = cleanName,
                    inviteCode = targetHouseholdId,
                    ownerUserId = ownerUser.uid,
                    lastSyncedTimestamp = now
                )
            )
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/${ownerUser.uid}")
            Result.failure(e)
        }
    }

    override suspend fun createInvitation(
        invitation: InvitationEntity,
        adminUids: List<String>
    ): Result<Unit> {
        val path = "invitations/${invitation.invitationTokenHash}"
        return try {
            val db = requireDb()
            val sanitizedAdmins = (adminUids + invitation.inviterUserId)
                .filter { it.isNotBlank() }
                .distinct()
                .take(20)

            val payload = hashMapOf<String, Any?>(
                "id" to invitation.id,
                "householdId" to invitation.householdId,
                "householdName" to invitation.householdName,
                "inviterUserId" to invitation.inviterUserId,
                "inviterName" to invitation.inviterName,
                "inviteeEmail" to InvitationTokenUtil.normalizeEmail(invitation.inviteeEmail),
                "inviteeName" to invitation.inviteeName.trim(),
                "personalMessage" to invitation.personalMessage.trim(),
                "invitationTokenHash" to invitation.invitationTokenHash,
                "role" to invitation.role,
                "status" to InvitationStatus.PENDING.value,
                "adminUids" to sanitizedAdmins,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(invitation.expiresAt)),
                "acceptedAt" to null,
                "acceptedByUserId" to "",
                "revokedAt" to null,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("invitations")
                .document(invitation.invitationTokenHash)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun extendInvitationExpiration(
        tokenHash: String,
        newExpiresAtMillis: Long
    ): Result<Unit> {
        val path = "invitations/$tokenHash"
        return try {
            val db = requireDb()
            db.collection("invitations")
                .document(tokenHash)
                .update(
                    mapOf(
                        "status" to InvitationStatus.PENDING.value,
                        "expiresAt" to Timestamp(Date(newExpiresAtMillis)),
                        "revokedAt" to null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun revokeInvitation(
        tokenHash: String,
        revokedAtMillis: Long
    ): Result<Unit> {
        val path = "invitations/$tokenHash"
        return try {
            val db = requireDb()
            db.collection("invitations")
                .document(tokenHash)
                .update(
                    mapOf(
                        "status" to InvitationStatus.REVOKED.value,
                        "revokedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun fetchInvitationByTokenHash(tokenHash: String): Result<InvitationEntity?> {
        val path = "invitations/$tokenHash"
        return try {
            val db = requireDb()
            val doc = db.collection("invitations").document(tokenHash).get().await()
            if (!doc.exists()) {
                return Result.success(null)
            }
            Result.success(doc.toInvitationEntity())
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    override suspend fun acceptInvitationTransaction(
        tokenHash: String,
        acceptingUser: AuthUser
    ): Result<AcceptedHouseholdResult> {
        val path = "invitations/$tokenHash"
        return try {
            val db = requireDb()
            val invRef = db.collection("invitations").document(tokenHash)
            val userRef = db.collection("users").document(acceptingUser.uid)

            val accepted = db.runTransaction { tx ->
                val invSnap = tx.get(invRef)
                if (!invSnap.exists()) {
                    throw InvitationRuleException("NOT_FOUND", "This invitation is no longer available.")
                }

                val invitation = invSnap.toInvitationEntity()
                val now = System.currentTimeMillis()
                when (invitation.effectiveStatus(now)) {
                    InvitationStatus.REVOKED ->
                        throw InvitationRuleException("REVOKED", "This invitation is no longer available.")
                    InvitationStatus.ACCEPTED ->
                        throw InvitationRuleException("ALREADY_USED", "This invitation has already been accepted and cannot be reused.")
                    InvitationStatus.EXPIRED ->
                        throw InvitationRuleException("EXPIRED", "This invitation has expired. Ask the inviter to send a new one.")
                    InvitationStatus.PENDING -> Unit
                }

                if (invitation.inviteeEmail.isNotBlank()) {
                    val expected = InvitationTokenUtil.normalizeEmail(invitation.inviteeEmail)
                    val actual = InvitationTokenUtil.normalizeEmail(acceptingUser.email)
                    if (actual != expected) {
                        throw InvitationRuleException(
                            "EMAIL_MISMATCH",
                            "This invitation was sent to $expected. Please sign in with that email address."
                        )
                    }
                }

                val memberRef = db.collection("households")
                    .document(invitation.householdId)
                    .collection("members")
                    .document(acceptingUser.uid)
                val existingMemberSnap = tx.get(memberRef)
                if (existingMemberSnap.exists() &&
                    existingMemberSnap.getString("membershipStatus") == MembershipStatus.ACTIVE.value
                ) {
                    throw InvitationRuleException(
                        "ALREADY_MEMBER",
                        "This person is already part of your household."
                    )
                }

                val assignedRole = HouseholdRole.fromString(invitation.role).let {
                    if (it == HouseholdRole.OWNER) HouseholdRole.MEMBER else it
                }

                val memberPayload = hashMapOf<String, Any>(
                    "id" to acceptingUser.uid,
                    "userId" to acceptingUser.uid,
                    "name" to acceptingUser.displayName.ifBlank {
                        invitation.inviteeName.ifBlank { "Family Member" }
                    },
                    "email" to InvitationTokenUtil.normalizeEmail(acceptingUser.email),
                    "role" to if (assignedRole == HouseholdRole.ADMIN) "Admin" else "Member",
                    "householdRole" to assignedRole.value,
                    "membershipStatus" to MembershipStatus.ACTIVE.value,
                    "avatarColorHex" to pickAvatarColor(acceptingUser.uid),
                    "avatarIcon" to "person",
                    "householdId" to invitation.householdId,
                    "monthlyContributionGoal" to 0.0,
                    "acceptedInvitationHash" to tokenHash,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(memberRef, memberPayload)

                tx.update(
                    invRef,
                    mapOf(
                        "status" to InvitationStatus.ACCEPTED.value,
                        "acceptedAt" to FieldValue.serverTimestamp(),
                        "acceptedByUserId" to acceptingUser.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    userRef,
                    hashMapOf<String, Any>(
                        "uid" to acceptingUser.uid,
                        "displayName" to acceptingUser.displayName.ifBlank { "Family Member" },
                        "email" to InvitationTokenUtil.normalizeEmail(acceptingUser.email),
                        "activeHouseholdId" to invitation.householdId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                AcceptedHouseholdResult(
                    householdId = invitation.householdId,
                    householdName = invitation.householdName,
                    assignedRole = assignedRole,
                    invitation = invitation.copy(
                        status = InvitationStatus.ACCEPTED.value,
                        acceptedAt = now,
                        acceptedByUserId = acceptingUser.uid
                    )
                )
            }.await()

            Result.success(accepted)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateMemberRole(
        householdId: String,
        memberId: String,
        newRole: HouseholdRole
    ): Result<Unit> {
        val path = "households/$householdId/members/$memberId"
        return try {
            val db = requireDb()
            db.collection("households")
                .document(householdId)
                .collection("members")
                .document(memberId)
                .update(
                    mapOf(
                        "householdRole" to newRole.value,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun removeMember(householdId: String, memberId: String): Result<Unit> {
        val path = "households/$householdId/members/$memberId"
        return try {
            val db = requireDb()
            db.collection("households")
                .document(householdId)
                .collection("members")
                .document(memberId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        private val avatarPalette = listOf(
            "#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#14B8A6"
        )

        fun pickAvatarColor(seed: String): String {
            val idx = kotlin.math.abs(seed.hashCode()) % avatarPalette.size
            return avatarPalette[idx]
        }

        private fun DocumentSnapshot.toInvitationEntity(): InvitationEntity {
            val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: System.currentTimeMillis()
            val expiresMs = getTimestamp("expiresAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: (createdMs + InvitationTokenUtil.DEFAULT_EXPIRATION_MILLIS)
            val acceptedMs = getTimestamp("acceptedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time
            val revokedMs = getTimestamp("revokedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time

            return InvitationEntity(
                id = getString("id") ?: id,
                householdId = getString("householdId") ?: "",
                householdName = getString("householdName") ?: "Family Household",
                inviterUserId = getString("inviterUserId") ?: "",
                inviterName = getString("inviterName") ?: "Family Member",
                inviteeEmail = getString("inviteeEmail") ?: "",
                inviteeName = getString("inviteeName") ?: "",
                personalMessage = getString("personalMessage") ?: "",
                invitationTokenHash = getString("invitationTokenHash") ?: id,
                role = getString("role") ?: HouseholdRole.MEMBER.value,
                status = getString("status") ?: InvitationStatus.PENDING.value,
                createdAt = createdMs,
                expiresAt = expiresMs,
                acceptedAt = acceptedMs,
                acceptedByUserId = getString("acceptedByUserId") ?: "",
                revokedAt = revokedMs
            )
        }
    }
}

/**
 * Unified Family Invitation & Member Management Repository.
 * Single source of truth used by BOTH the Home screen Invite button and the Family tab Add Member button.
 */
class FamilyInvitationRepository(
    private val invitationDao: InvitationDao,
    private val familyMemberDao: FamilyMemberDao,
    private val householdDao: HouseholdDao,
    private val cloudBackend: InvitationCloudBackend
) {
    /**
     * In-memory ephemeral cache mapping invitationId -> raw shareable link for the current session.
     * Raw bearer tokens are intentionally NEVER persisted to SQLite or Firestore.
     */
    private val ephemeralLinkCache = ConcurrentHashMap<String, String>()

    fun observeInvitations(householdId: String): Flow<List<InvitationEntity>> {
        return invitationDao.getInvitationsByHousehold(householdId)
    }

    fun getCachedShareLink(invitationId: String): String? = ephemeralLinkCache[invitationId]

    suspend fun bootstrapAuthenticatedHousehold(
        localHousehold: HouseholdEntity,
        user: AuthUser
    ): Result<HouseholdEntity> {
        val result = cloudBackend.resolveOrBootstrapHousehold(localHousehold, user)
        result.onSuccess { resolved ->
            householdDao.insertHousehold(resolved)
        }
        return result
    }

    /**
     * Unified invitation creator for BOTH "Invite via Email" and "Share Invitation Link".
     * Only confirms success after backend persistence succeeds.
     */
    suspend fun createInvitation(
        householdId: String,
        householdName: String,
        inviter: AuthUser?,
        inviterRole: HouseholdRole,
        inviteeEmail: String?,
        inviteeName: String = "",
        personalMessage: String = "",
        invitedRole: HouseholdRole = HouseholdRole.MEMBER,
        expirationMillis: Long = InvitationTokenUtil.DEFAULT_EXPIRATION_MILLIS
    ): InvitationCreateResult {
        if (inviter == null) {
            return InvitationCreateResult.Error("Please sign in with Google before creating a family invitation.")
        }
        if (!inviterRole.canInviteMembers()) {
            return InvitationCreateResult.Error("Only household Owners and Admins can invite new members.")
        }

        // Prevent escalating to OWNER via invitation
        val safeRole = if (invitedRole == HouseholdRole.ADMIN) HouseholdRole.ADMIN else HouseholdRole.MEMBER
        val isEmailInvite = inviteeEmail != null
        val normalizedEmail = if (isEmailInvite) {
            val trimmed = inviteeEmail.trim()
            if (trimmed.isEmpty()) {
                return InvitationCreateResult.Error("Please enter an email address.")
            }
            if (!InvitationTokenUtil.isValidEmail(trimmed)) {
                return InvitationCreateResult.Error("Please enter a valid email address.")
            }
            InvitationTokenUtil.normalizeEmail(trimmed)
        } else {
            ""
        }

        if (isEmailInvite) {
            if (normalizedEmail.equals(InvitationTokenUtil.normalizeEmail(inviter.email), ignoreCase = true)) {
                return InvitationCreateResult.Error("This person is already part of your household.")
            }

            val existingMember = familyMemberDao.getMemberByEmail(householdId, normalizedEmail)
            if (existingMember != null && existingMember.parsedMembershipStatus() == MembershipStatus.ACTIVE) {
                return InvitationCreateResult.Error("This person is already part of your household.")
            }

            val existingPending = invitationDao.findActivePendingByEmail(
                householdId = householdId,
                email = normalizedEmail,
                nowMillis = System.currentTimeMillis()
            )
            if (existingPending != null) {
                return InvitationCreateResult.Error("An active invitation is already pending for $normalizedEmail.")
            }
        }

        val now = System.currentTimeMillis()
        val rawToken = InvitationTokenUtil.generateSecureToken()
        val tokenHash = InvitationTokenUtil.sha256Hex(rawToken)
        val shareLink = InvitationTokenUtil.buildInvitationLink(rawToken)

        val members = familyMemberDao.getMembersByHouseholdOnce(householdId)
        val adminUids = members
            .filter {
                it.userId.isNotBlank() &&
                    it.parsedMembershipStatus() == MembershipStatus.ACTIVE &&
                    it.parsedHouseholdRole().canManageInvitations()
            }
            .map { it.userId }
            .plus(inviter.uid)
            .distinct()

        val invitation = InvitationEntity(
            id = UUID.randomUUID().toString(),
            householdId = householdId,
            householdName = householdName.ifBlank { "My Household" },
            inviterUserId = inviter.uid,
            inviterName = inviter.displayName.ifBlank { "Family Member" },
            inviteeEmail = normalizedEmail,
            inviteeName = inviteeName.trim(),
            personalMessage = personalMessage.trim(),
            invitationTokenHash = tokenHash,
            role = safeRole.value,
            status = InvitationStatus.PENDING.value,
            createdAt = now,
            expiresAt = now + expirationMillis.coerceAtLeast(60_000L)
        )

        val backendResult = cloudBackend.createInvitation(invitation, adminUids)
        if (backendResult.isFailure) {
            val reason = backendResult.exceptionOrNull()?.localizedMessage
                ?: "Could not create invitation. Please try again."
            return InvitationCreateResult.Error(reason)
        }

        // Backend confirmed creation -> cache metadata locally & hold ephemeral share link in RAM only
        invitationDao.insertInvitation(invitation)
        ephemeralLinkCache[invitation.id] = shareLink

        return InvitationCreateResult.Success(
            invitation = invitation,
            shareLink = shareLink,
            emailDeliveryConfigured = false
        )
    }

    /**
     * Resends an invitation (extends expiration if link is in memory and still active,
     * or securely rotates to a fresh single-use token & hash if expired or link is unavailable).
     */
    suspend fun resendInvitation(
        invitationId: String,
        caller: AuthUser?,
        callerRole: HouseholdRole
    ): InvitationActionResult {
        if (caller == null) {
            return InvitationActionResult.Error("Please sign in with Google to manage invitations.")
        }
        if (!callerRole.canManageInvitations()) {
            return InvitationActionResult.Error("Only household Owners and Admins can resend invitations.")
        }

        val existing = invitationDao.getInvitationById(invitationId)
            ?: return InvitationActionResult.Error("Invitation not found.")

        val status = existing.effectiveStatus()
        if (status == InvitationStatus.ACCEPTED) {
            return InvitationActionResult.Error("This invitation has already been accepted.")
        }

        val cachedLink = ephemeralLinkCache[existing.id]
        val now = System.currentTimeMillis()
        val newExpiresAt = now + InvitationTokenUtil.DEFAULT_EXPIRATION_MILLIS

        if (status == InvitationStatus.PENDING && cachedLink != null) {
            val extendResult = cloudBackend.extendInvitationExpiration(existing.invitationTokenHash, newExpiresAt)
            if (extendResult.isFailure) {
                return InvitationActionResult.Error(
                    extendResult.exceptionOrNull()?.localizedMessage ?: "Failed to refresh invitation."
                )
            }
            invitationDao.updateInvitation(
                existing.copy(status = InvitationStatus.PENDING.value, expiresAt = newExpiresAt)
            )
            return InvitationActionResult.Success(
                message = "Invitation refreshed! Share the updated link.",
                updatedShareLink = cachedLink
            )
        }

        // Rotate to a brand-new cryptographically secure token if expired, revoked, or from a previous session
        if (existing.status == InvitationStatus.PENDING.value) {
            cloudBackend.revokeInvitation(existing.invitationTokenHash, now)
            invitationDao.updateInvitation(
                existing.copy(status = InvitationStatus.REVOKED.value, revokedAt = now)
            )
            ephemeralLinkCache.remove(existing.id)
        }

        return when (
            val recreated = createInvitation(
                householdId = existing.householdId,
                householdName = existing.householdName,
                inviter = caller,
                inviterRole = callerRole,
                inviteeEmail = existing.inviteeEmail.ifBlank { null },
                inviteeName = existing.inviteeName,
                personalMessage = existing.personalMessage,
                invitedRole = HouseholdRole.fromString(existing.role)
            )
        ) {
            is InvitationCreateResult.Success -> InvitationActionResult.Success(
                message = "New invitation link generated and ready to share.",
                updatedShareLink = recreated.shareLink
            )
            is InvitationCreateResult.Error -> InvitationActionResult.Error(recreated.message)
        }
    }

    suspend fun revokeInvitation(
        invitationId: String,
        caller: AuthUser?,
        callerRole: HouseholdRole
    ): InvitationActionResult {
        if (caller == null) {
            return InvitationActionResult.Error("Please sign in with Google to revoke invitations.")
        }
        if (!callerRole.canManageInvitations()) {
            return InvitationActionResult.Error("Only household Owners and Admins can revoke invitations.")
        }

        val existing = invitationDao.getInvitationById(invitationId)
            ?: return InvitationActionResult.Error("Invitation not found.")

        if (existing.effectiveStatus() == InvitationStatus.ACCEPTED) {
            return InvitationActionResult.Error("Accepted invitations cannot be revoked.")
        }
        if (existing.effectiveStatus() == InvitationStatus.REVOKED) {
            return InvitationActionResult.Error("This invitation has already been revoked.")
        }

        val now = System.currentTimeMillis()
        val backendResult = cloudBackend.revokeInvitation(existing.invitationTokenHash, now)
        if (backendResult.isFailure) {
            return InvitationActionResult.Error(
                backendResult.exceptionOrNull()?.localizedMessage ?: "Failed to revoke invitation."
            )
        }

        invitationDao.updateInvitation(
            existing.copy(status = InvitationStatus.REVOKED.value, revokedAt = now)
        )
        ephemeralLinkCache.remove(existing.id)
        return InvitationActionResult.Success("Invitation revoked.")
    }

    /**
     * Validates an incoming invitation link or token without exposing any private household financial data.
     */
    suspend fun validateInvitationToken(
        rawInput: String,
        currentUser: AuthUser?
    ): InvitationValidationResult {
        val rawToken = InvitationTokenUtil.extractTokenFromInput(rawInput)
            ?: return InvitationValidationResult.Invalid("This invitation link is invalid or incomplete.")

        if (currentUser == null) {
            return InvitationValidationResult.RequiresSignIn(rawToken)
        }

        val tokenHash = InvitationTokenUtil.sha256Hex(rawToken)
        val cloudResult = cloudBackend.fetchInvitationByTokenHash(tokenHash)
        val invitation = if (cloudResult.isSuccess) {
            cloudResult.getOrNull() ?: invitationDao.getInvitationByTokenHash(tokenHash)
        } else {
            invitationDao.getInvitationByTokenHash(tokenHash)
                ?: return InvitationValidationResult.Invalid(
                    cloudResult.exceptionOrNull()?.localizedMessage ?: "Unable to verify invitation right now."
                )
        }

        if (invitation == null) {
            return InvitationValidationResult.Invalid("This invitation is no longer available.")
        }

        return when (invitation.effectiveStatus()) {
            InvitationStatus.REVOKED ->
                InvitationValidationResult.Revoked("This invitation is no longer available.")
            InvitationStatus.ACCEPTED ->
                InvitationValidationResult.AlreadyUsed("This invitation has already been accepted and cannot be reused.")
            InvitationStatus.EXPIRED ->
                InvitationValidationResult.Expired(
                    "This invitation has expired. Ask the inviter to send a new one.",
                    invitation
                )
            InvitationStatus.PENDING ->
                InvitationValidationResult.Valid(invitation, rawToken)
        }
    }

    /**
     * Atomically accepts a validated invitation, creates membership, marks token single-use ACCEPTED,
     * and updates local household records.
     */
    suspend fun acceptInvitation(
        rawToken: String,
        currentUser: AuthUser?
    ): InvitationAcceptResult {
        if (currentUser == null) {
            return InvitationAcceptResult.RequiresSignIn
        }
        val cleanToken = InvitationTokenUtil.extractTokenFromInput(rawToken)
            ?: return InvitationAcceptResult.Error("This invitation is no longer available.")

        val tokenHash = InvitationTokenUtil.sha256Hex(cleanToken)
        val txResult = cloudBackend.acceptInvitationTransaction(tokenHash, currentUser)
        if (txResult.isFailure) {
            val ex = txResult.exceptionOrNull()
            return if (ex is InvitationRuleException) {
                when (ex.code) {
                    "ALREADY_MEMBER" -> InvitationAcceptResult.AlreadyMember(ex.message)
                    "EMAIL_MISMATCH" -> InvitationAcceptResult.EmailMismatch(
                        expectedEmail = ex.message,
                        actualEmail = currentUser.email
                    )
                    else -> InvitationAcceptResult.Error(ex.message)
                }
            } else {
                InvitationAcceptResult.Error(
                    ex?.localizedMessage ?: "Could not accept invitation. Please try again."
                )
            }
        }

        val accepted = txResult.getOrThrow()
        val now = System.currentTimeMillis()

        // Persist updated household, membership, and accepted invitation locally
        householdDao.insertHousehold(
            HouseholdEntity(
                householdId = accepted.householdId,
                householdName = accepted.householdName,
                inviteCode = accepted.householdId,
                lastSyncedTimestamp = now,
                isLiveSyncEnabled = true
            )
        )
        familyMemberDao.insertMember(
            FamilyMemberEntity(
                id = currentUser.uid,
                name = currentUser.displayName,
                role = if (accepted.assignedRole == HouseholdRole.ADMIN) "Admin" else "Member",
                avatarColorHex = FirestoreInvitationCloudBackend.pickAvatarColor(currentUser.uid),
                avatarIcon = "person",
                isCurrentActiveUser = true,
                householdId = accepted.householdId,
                email = InvitationTokenUtil.normalizeEmail(currentUser.email),
                userId = currentUser.uid,
                householdRole = accepted.assignedRole.value,
                membershipStatus = MembershipStatus.ACTIVE.value,
                joinedAt = now
            )
        )
        familyMemberDao.setActiveMember(accepted.householdId, currentUser.uid)
        invitationDao.insertInvitation(accepted.invitation)

        return InvitationAcceptResult.Accepted(
            householdId = accepted.householdId,
            householdName = accepted.householdName,
            role = accepted.assignedRole
        )
    }

    suspend fun updateMemberRole(
        householdId: String,
        targetMember: FamilyMemberEntity,
        newRole: HouseholdRole,
        callerUserId: String?,
        callerRole: HouseholdRole
    ): MemberManagementResult {
        if (callerUserId.isNullOrBlank()) {
            return MemberManagementResult.Error("Please sign in with Google to manage member roles.")
        }
        val isSelf = targetMember.userId == callerUserId || targetMember.id == callerUserId
        if (!callerRole.canChangeRole(targetMember.parsedHouseholdRole(), newRole, isSelf)) {
            return MemberManagementResult.Error(
                if (isSelf) "You cannot change your own household role."
                else "Only the household Owner can update member roles."
            )
        }

        val result = cloudBackend.updateMemberRole(householdId, targetMember.id, newRole)
        if (result.isFailure) {
            return MemberManagementResult.Error(
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update role."
            )
        }

        familyMemberDao.updateMember(targetMember.copy(householdRole = newRole.value))
        return MemberManagementResult.Success("Updated ${targetMember.name}'s role to ${newRole.displayLabel}.")
    }

    suspend fun removeMember(
        householdId: String,
        targetMember: FamilyMemberEntity,
        callerUserId: String?,
        callerRole: HouseholdRole
    ): MemberManagementResult {
        val isSelf = !callerUserId.isNullOrBlank() &&
            (targetMember.userId == callerUserId || targetMember.id == callerUserId)
        if (!callerRole.canRemoveMember(targetMember.parsedHouseholdRole(), isSelf)) {
            return MemberManagementResult.Error(
                if (targetMember.parsedHouseholdRole() == HouseholdRole.OWNER) {
                    "The household Owner cannot be removed."
                } else {
                    "You do not have permission to remove ${targetMember.name}."
                }
            )
        }

        if (!callerUserId.isNullOrBlank()) {
            val result = cloudBackend.removeMember(householdId, targetMember.id)
            if (result.isFailure) {
                return MemberManagementResult.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to remove member."
                )
            }
        }

        familyMemberDao.deleteMember(targetMember)
        return MemberManagementResult.Success("Removed ${targetMember.name} from the household.")
    }
}
