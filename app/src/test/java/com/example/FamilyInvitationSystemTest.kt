package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AuthUser
import com.example.data.ExpenseRepository
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.InvitationEntity
import com.example.data.model.InvitationStatus
import com.example.data.model.MembershipStatus
import com.example.data.repository.AcceptedHouseholdResult
import com.example.data.repository.FamilyInvitationRepository
import com.example.data.repository.InvitationAcceptResult
import com.example.data.repository.InvitationActionResult
import com.example.data.repository.InvitationCloudBackend
import com.example.data.repository.InvitationCreateResult
import com.example.data.repository.InvitationRuleException
import com.example.data.repository.InvitationValidationResult
import com.example.data.repository.MemberManagementResult
import com.example.ui.components.FamilyMemberManagementSheet
import com.example.ui.components.InviteFamilySheet
import com.example.ui.screens.FamilySyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.FamSpendTheme
import com.example.util.InvitationTokenUtil
import com.example.viewmodel.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory transactional backend matching the exact semantics of FirestoreInvitationCloudBackend
 * for deterministic multi-device and edge-case testing (TEST 1 - TEST 14).
 */
private class InMemoryTransactionalCloudBackend : InvitationCloudBackend {
    private val lock = Any()
    val households = ConcurrentHashMap<String, HouseholdEntity>()
    val membersByHousehold = ConcurrentHashMap<String, MutableMap<String, FamilyMemberEntity>>()
    val invitationsByHash = ConcurrentHashMap<String, InvitationEntity>()

    override suspend fun resolveOrBootstrapHousehold(
        localHousehold: HouseholdEntity,
        ownerUser: AuthUser
    ): Result<HouseholdEntity> = synchronized(lock) {
        val hId = if (localHousehold.householdId == "FAM-1001") {
            "FAM-${ownerUser.uid.take(8).uppercase()}"
        } else {
            localHousehold.householdId
        }
        val h = localHousehold.copy(householdId = hId, inviteCode = hId, ownerUserId = ownerUser.uid)
        households.putIfAbsent(hId, h)
        val map = membersByHousehold.getOrPut(hId) { mutableMapOf() }
        map.putIfAbsent(
            ownerUser.uid,
            FamilyMemberEntity(
                id = ownerUser.uid,
                name = ownerUser.displayName,
                role = "Primary",
                avatarColorHex = "#4F46E5",
                householdId = hId,
                email = InvitationTokenUtil.normalizeEmail(ownerUser.email),
                userId = ownerUser.uid,
                householdRole = HouseholdRole.OWNER.value,
                membershipStatus = MembershipStatus.ACTIVE.value,
                joinedAt = System.currentTimeMillis()
            )
        )
        Result.success(households[hId]!!)
    }

    override suspend fun createInvitation(
        invitation: InvitationEntity,
        adminUids: List<String>
    ): Result<Unit> = synchronized(lock) {
        invitationsByHash[invitation.invitationTokenHash] = invitation
        Result.success(Unit)
    }

    override suspend fun extendInvitationExpiration(
        tokenHash: String,
        newExpiresAtMillis: Long
    ): Result<Unit> = synchronized(lock) {
        val existing = invitationsByHash[tokenHash]
            ?: return@synchronized Result.failure(IllegalArgumentException("Not found"))
        invitationsByHash[tokenHash] = existing.copy(
            status = InvitationStatus.PENDING.value,
            expiresAt = newExpiresAtMillis
        )
        Result.success(Unit)
    }

    override suspend fun revokeInvitation(tokenHash: String, revokedAtMillis: Long): Result<Unit> = synchronized(lock) {
        val existing = invitationsByHash[tokenHash]
            ?: return@synchronized Result.failure(IllegalArgumentException("Not found"))
        invitationsByHash[tokenHash] = existing.copy(
            status = InvitationStatus.REVOKED.value,
            revokedAt = revokedAtMillis
        )
        Result.success(Unit)
    }

    override suspend fun fetchInvitationByTokenHash(tokenHash: String): Result<InvitationEntity?> = synchronized(lock) {
        Result.success(invitationsByHash[tokenHash])
    }

    override suspend fun acceptInvitationTransaction(
        tokenHash: String,
        acceptingUser: AuthUser
    ): Result<AcceptedHouseholdResult> = synchronized(lock) {
        val inv = invitationsByHash[tokenHash]
            ?: return@synchronized Result.failure(InvitationRuleException("NOT_FOUND", "This invitation is no longer available."))

        when (inv.effectiveStatus()) {
            InvitationStatus.REVOKED ->
                return@synchronized Result.failure(InvitationRuleException("REVOKED", "This invitation is no longer available."))
            InvitationStatus.ACCEPTED ->
                return@synchronized Result.failure(InvitationRuleException("ALREADY_USED", "This invitation has already been accepted and cannot be reused."))
            InvitationStatus.EXPIRED ->
                return@synchronized Result.failure(InvitationRuleException("EXPIRED", "This invitation has expired. Ask the inviter to send a new one."))
            InvitationStatus.PENDING -> Unit
        }

        if (inv.inviteeEmail.isNotBlank()) {
            val expected = InvitationTokenUtil.normalizeEmail(inv.inviteeEmail)
            val actual = InvitationTokenUtil.normalizeEmail(acceptingUser.email)
            if (expected != actual) {
                return@synchronized Result.failure(
                    InvitationRuleException("EMAIL_MISMATCH", "This invitation was sent to $expected.")
                )
            }
        }

        val members = membersByHousehold.getOrPut(inv.householdId) { mutableMapOf() }
        if (members.containsKey(acceptingUser.uid)) {
            return@synchronized Result.failure(
                InvitationRuleException("ALREADY_MEMBER", "This person is already part of your household.")
            )
        }

        val role = HouseholdRole.fromString(inv.role).let {
            if (it == HouseholdRole.OWNER) HouseholdRole.MEMBER else it
        }
        val now = System.currentTimeMillis()
        members[acceptingUser.uid] = FamilyMemberEntity(
            id = acceptingUser.uid,
            name = acceptingUser.displayName,
            role = role.displayLabel,
            avatarColorHex = "#10B981",
            householdId = inv.householdId,
            email = InvitationTokenUtil.normalizeEmail(acceptingUser.email),
            userId = acceptingUser.uid,
            householdRole = role.value,
            membershipStatus = MembershipStatus.ACTIVE.value,
            joinedAt = now
        )

        val acceptedInv = inv.copy(
            status = InvitationStatus.ACCEPTED.value,
            acceptedAt = now,
            acceptedByUserId = acceptingUser.uid
        )
        invitationsByHash[tokenHash] = acceptedInv

        Result.success(
            AcceptedHouseholdResult(
                householdId = inv.householdId,
                householdName = inv.householdName,
                assignedRole = role,
                invitation = acceptedInv
            )
        )
    }

    override suspend fun updateMemberRole(
        householdId: String,
        memberId: String,
        newRole: HouseholdRole
    ): Result<Unit> = synchronized(lock) {
        val map = membersByHousehold[householdId] ?: return@synchronized Result.failure(Exception("Household missing"))
        val existing = map[memberId] ?: return@synchronized Result.failure(Exception("Member missing"))
        map[memberId] = existing.copy(householdRole = newRole.value)
        Result.success(Unit)
    }

    override suspend fun removeMember(householdId: String, memberId: String): Result<Unit> = synchronized(lock) {
        membersByHousehold[householdId]?.remove(memberId)
        Result.success(Unit)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FamilyInvitationSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dbA: AppDatabase
    private lateinit var dbB: AppDatabase
    private lateinit var sharedCloud: InMemoryTransactionalCloudBackend
    private lateinit var repoA: FamilyInvitationRepository
    private lateinit var repoB: FamilyInvitationRepository

    private val ownerUser = AuthUser(
        uid = "uid_owner_a",
        displayName = "Aarav Sharma",
        email = "aarav@example.com",
        isEmailVerified = true
    )
    private val recipientUser = AuthUser(
        uid = "uid_member_b",
        displayName = "Priya Sharma",
        email = "priya@example.com",
        isEmailVerified = true
    )
    private val householdId = "FAM-SHARMA01"

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        dbA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dbB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sharedCloud = InMemoryTransactionalCloudBackend()

        repoA = FamilyInvitationRepository(
            dbA.invitationDao(),
            dbA.familyMemberDao(),
            dbA.householdDao(),
            sharedCloud
        )
        repoB = FamilyInvitationRepository(
            dbB.invitationDao(),
            dbB.familyMemberDao(),
            dbB.householdDao(),
            sharedCloud
        )

        val household = HouseholdEntity(
            householdId = householdId,
            householdName = "Sharma Family",
            inviteCode = householdId,
            ownerUserId = ownerUser.uid
        )
        repoA.bootstrapAuthenticatedHousehold(household, ownerUser)
        dbA.familyMemberDao().insertMember(
            FamilyMemberEntity(
                id = ownerUser.uid,
                name = ownerUser.displayName,
                role = "Primary",
                avatarColorHex = "#4F46E5",
                isCurrentActiveUser = true,
                householdId = householdId,
                email = ownerUser.email,
                userId = ownerUser.uid,
                householdRole = HouseholdRole.OWNER.value,
                membershipStatus = MembershipStatus.ACTIVE.value,
                joinedAt = System.currentTimeMillis()
            )
        )
    }

    @After
    fun tearDown() {
        dbA.close()
        dbB.close()
    }

    // TEST 1 & TEST 3: Home Screen existing Invite button opens unified "Invite your family" modal
    @Test
    fun test01_homeScreenInviteButtonOpensUnifiedInviteSheet() {
        var isInviteSheetOpen by mutableStateOf(false)
        val uiState = UiState(
            householdId = householdId,
            householdName = "Sharma Family",
            authUser = ownerUser,
            currentUserHouseholdRole = HouseholdRole.OWNER,
            members = listOf(
                FamilyMemberEntity(
                    id = ownerUser.uid,
                    name = ownerUser.displayName,
                    role = "Primary",
                    avatarColorHex = "#4F46E5",
                    isCurrentActiveUser = true,
                    householdId = householdId,
                    email = ownerUser.email,
                    userId = ownerUser.uid,
                    householdRole = HouseholdRole.OWNER.value
                )
            )
        )

        composeTestRule.setContent {
            FamSpendTheme {
                HomeScreen(
                    uiState = uiState,
                    onAddExpenseClick = {},
                    onSyncNowClick = {},
                    onSimulateLiveClick = {},
                    onMonthlySummaryClick = {},
                    onViewAllTransactions = {},
                    onEditExpense = {},
                    onViewExpense = {},
                    onDeleteExpense = {},
                    onToggleSelection = {},
                    onDismissBudgetAlert = {},
                    onEditBudgetClick = {},
                    onAddMemberClick = { isInviteSheetOpen = true }
                )
                if (isInviteSheetOpen) {
                    InviteFamilySheet(
                        householdName = uiState.householdName,
                        authUser = ownerUser,
                        isAuthLoading = false,
                        currentUserRole = HouseholdRole.OWNER,
                        isSubmitting = false,
                        errorMessage = null,
                        createdInvitation = null,
                        createdShareLink = null,
                        emailDeliveryConfigured = false,
                        onDismiss = { isInviteSheetOpen = false },
                        onSignInClick = {},
                        onClearError = {},
                        onSendEmailInvite = { _, _, _, _ -> },
                        onGenerateShareLink = { _, _ -> },
                        onInviteAnother = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodes(hasScrollToKeyAction()).onFirst()
            .performScrollToKey("household_members_section")
        composeTestRule.onNodeWithTag("home_invite_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Invite your family").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage money together with your family.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_method_email_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_method_link_tab").assertIsDisplayed()
    }

    // TEST 2 & TEST 3: Family Tab existing Add Member button opens Member Management sheet & routes Add New Member to unified Invite sheet
    @Test
    fun test02_familyTabAddMemberOpensManagementSheetAndUnifiedInviteFlow() {
        var isMemberMgmtOpen by mutableStateOf(false)
        var isInviteSheetOpen by mutableStateOf(false)
        val pendingInvite = InvitationEntity(
            id = "inv_ui_1",
            householdId = householdId,
            householdName = "Sharma Family",
            inviterUserId = ownerUser.uid,
            inviterName = ownerUser.displayName,
            inviteeEmail = "priya@example.com",
            inviteeName = "Priya",
            invitationTokenHash = "f".repeat(64),
            role = HouseholdRole.MEMBER.value,
            status = InvitationStatus.PENDING.value,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + InvitationTokenUtil.DEFAULT_EXPIRATION_MILLIS
        )
        val uiState = UiState(
            householdId = householdId,
            householdName = "Sharma Family",
            authUser = ownerUser,
            currentUserHouseholdRole = HouseholdRole.OWNER,
            members = listOf(
                FamilyMemberEntity(
                    id = ownerUser.uid,
                    name = ownerUser.displayName,
                    role = "Primary",
                    avatarColorHex = "#4F46E5",
                    isCurrentActiveUser = true,
                    householdId = householdId,
                    email = ownerUser.email,
                    userId = ownerUser.uid,
                    householdRole = HouseholdRole.OWNER.value
                )
            ),
            invitations = listOf(pendingInvite)
        )

        composeTestRule.setContent {
            FamSpendTheme {
                FamilySyncScreen(
                    uiState = uiState,
                    onAddMemberClick = { isMemberMgmtOpen = true },
                    onSwitchActiveMember = {},
                    onToggleLiveSync = {},
                    onSyncNowClick = {},
                    onSimulateLiveClick = {},
                    onJoinHouseholdClick = {}
                )
                if (isMemberMgmtOpen) {
                    FamilyMemberManagementSheet(
                        householdName = uiState.householdName,
                        authUser = ownerUser,
                        currentUserRole = HouseholdRole.OWNER,
                        members = uiState.members,
                        activeMemberId = ownerUser.uid,
                        invitations = uiState.invitations,
                        isInvitationsLoading = false,
                        feedbackMessage = null,
                        getCachedLink = { null },
                        onDismiss = { isMemberMgmtOpen = false },
                        onClearFeedback = {},
                        onAddNewMemberClick = {
                            isMemberMgmtOpen = false
                            isInviteSheetOpen = true
                        },
                        onAcceptInviteLinkClick = {},
                        onSwitchActiveMember = {},
                        onEditMember = {},
                        onChangeMemberRole = { _, _ -> },
                        onRemoveMember = {},
                        onResendInvitation = { _, _ -> },
                        onRevokeInvitation = {}
                    )
                }
                if (isInviteSheetOpen) {
                    InviteFamilySheet(
                        householdName = uiState.householdName,
                        authUser = ownerUser,
                        isAuthLoading = false,
                        currentUserRole = HouseholdRole.OWNER,
                        isSubmitting = false,
                        errorMessage = null,
                        createdInvitation = null,
                        createdShareLink = null,
                        emailDeliveryConfigured = false,
                        onDismiss = { isInviteSheetOpen = false },
                        onSignInClick = {},
                        onClearError = {},
                        onSendEmailInvite = { _, _, _, _ -> },
                        onGenerateShareLink = { _, _ -> },
                        onInviteAnother = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("add_family_member_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Current Family Members").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Invitations").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("member_mgmt_add_new_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Invite your family").assertIsDisplayed()
    }

    // TEST 4: Email validation, duplicate prevention, existing member check, CSPRNG token & SHA-256 hash security
    @Test
    fun test04_emailValidationDuplicateBlockingAndHashedTokenStorage() = runBlocking {
        // Empty email rejected
        val emptyRes = repoA.createInvitation(householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, "")
        assertTrue(emptyRes is InvitationCreateResult.Error)

        // Malformed email rejected
        val badEmailRes = repoA.createInvitation(householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, "not-an-email")
        assertTrue(badEmailRes is InvitationCreateResult.Error)

        // Inviting self (existing member) rejected
        val selfRes = repoA.createInvitation(householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, "aarav@example.com")
        assertTrue(selfRes is InvitationCreateResult.Error)
        assertEquals("This person is already part of your household.", (selfRes as InvitationCreateResult.Error).message)

        // Valid email invitation succeeds
        val created = repoA.createInvitation(
            householdId = householdId,
            householdName = "Sharma Family",
            inviter = ownerUser,
            inviterRole = HouseholdRole.OWNER,
            inviteeEmail = "Priya@Example.com",
            inviteeName = "Priya",
            personalMessage = "Join our family!"
        )
        assertTrue(created is InvitationCreateResult.Success)
        val success = created as InvitationCreateResult.Success
        assertFalse(success.emailDeliveryConfigured)
        val rawToken = InvitationTokenUtil.extractTokenFromInput(success.shareLink)
        assertNotNull(rawToken)
        // Stored hash is SHA-256 hex (64 chars), NEVER raw token
        assertEquals(64, success.invitation.invitationTokenHash.length)
        assertNotEquals(rawToken, success.invitation.invitationTokenHash)
        assertEquals(InvitationTokenUtil.sha256Hex(rawToken!!), success.invitation.invitationTokenHash)

        // Duplicate active pending invitation for same email blocked
        val dupRes = repoA.createInvitation(
            householdId = householdId,
            householdName = "Sharma Family",
            inviter = ownerUser,
            inviterRole = HouseholdRole.OWNER,
            inviteeEmail = "priya@example.com"
        )
        assertTrue(dupRes is InvitationCreateResult.Error)

        // Shareable link generates unique unpredictable token each time
        val link1 = repoA.createInvitation(householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, null) as InvitationCreateResult.Success
        val link2 = repoA.createInvitation(householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, null) as InvitationCreateResult.Success
        assertNotEquals(link1.shareLink, link2.shareLink)
        assertNotEquals(link1.invitation.invitationTokenHash, link2.invitation.invitationTokenHash)
    }

    // TEST 5, 6, 7, 8 & 13: Unauthenticated preservation, token validation, atomic acceptance, single-use & cross-device sync
    @Test
    fun test05_06_07_08_13_fullRecipientAcceptanceAndCrossDeviceConsistency() = runBlocking {
        val created = repoA.createInvitation(
            householdId = householdId,
            householdName = "Sharma Family",
            inviter = ownerUser,
            inviterRole = HouseholdRole.OWNER,
            inviteeEmail = recipientUser.email,
            inviteeName = "Priya",
            invitedRole = HouseholdRole.MEMBER
        ) as InvitationCreateResult.Success

        // TEST 7: Unauthenticated recipient gets RequiresSignIn with preserved token
        val unauthCheck = repoB.validateInvitationToken(created.shareLink, currentUser = null)
        assertTrue(unauthCheck is InvitationValidationResult.RequiresSignIn)
        val preservedToken = (unauthCheck as InvitationValidationResult.RequiresSignIn).rawToken

        // TEST 5: Authenticated recipient validates preserved token
        val validCheck = repoB.validateInvitationToken(preservedToken, currentUser = recipientUser)
        assertTrue(validCheck is InvitationValidationResult.Valid)

        // Email mismatch is rejected
        val wrongUser = AuthUser("uid_wrong", "Stranger", "stranger@example.com")
        val mismatchRes = repoB.acceptInvitation(preservedToken, wrongUser)
        assertTrue(mismatchRes is InvitationAcceptResult.EmailMismatch)

        // TEST 6: Recipient accepts atomically
        val acceptRes = repoB.acceptInvitation(preservedToken, recipientUser)
        assertTrue(acceptRes is InvitationAcceptResult.Accepted)
        val accepted = acceptRes as InvitationAcceptResult.Accepted
        assertEquals(householdId, accepted.householdId)
        assertEquals(HouseholdRole.MEMBER, accepted.role)

        // TEST 8 & 13: Member record created on User B & cloud backend has both Owner and Priya with zero duplicates
        val bMembers = dbB.familyMemberDao().getMembersByHousehold(householdId).first()
        assertTrue(bMembers.any { it.userId == recipientUser.uid && it.email == "priya@example.com" })
        val cloudMembers = sharedCloud.membersByHousehold[householdId]!!
        assertEquals(2, cloudMembers.size)
        assertTrue(cloudMembers.containsKey(ownerUser.uid))
        assertTrue(cloudMembers.containsKey(recipientUser.uid))

        // Single-use enforcement: reusing token fails
        val reuseCheck = repoB.validateInvitationToken(preservedToken, recipientUser)
        assertTrue(reuseCheck is InvitationValidationResult.AlreadyUsed)
        val reuseAccept = repoB.acceptInvitation(preservedToken, recipientUser)
        assertTrue(reuseAccept is InvitationAcceptResult.Error)
    }

    // TEST 9, 10 & 11: Pending Invitations list, Resend, Revoke, Expired & Revoked rejection
    @Test
    fun test09_10_11_pendingInvitationsResendRevokeAndExpiration() = runBlocking {
        val created = repoA.createInvitation(
            householdId = householdId,
            householdName = "Sharma Family",
            inviter = ownerUser,
            inviterRole = HouseholdRole.OWNER,
            inviteeEmail = "rohan@example.com",
            inviteeName = "Rohan"
        ) as InvitationCreateResult.Success

        // Pending invitation appears in Room list
        val pendingList = repoA.observeInvitations(householdId).first()
        assertEquals(1, pendingList.size)
        assertEquals(InvitationStatus.PENDING, pendingList.first().effectiveStatus())

        // Resend extends expiration and preserves usable share link
        val resendRes = repoA.resendInvitation(created.invitation.id, ownerUser, HouseholdRole.OWNER)
        assertTrue(resendRes is InvitationActionResult.Success)
        assertNotNull((resendRes as InvitationActionResult.Success).updatedShareLink)

        // Revoke invalidates token
        val revokeRes = repoA.revokeInvitation(created.invitation.id, ownerUser, HouseholdRole.OWNER)
        assertTrue(revokeRes is InvitationActionResult.Success)
        val afterRevoke = dbA.invitationDao().getInvitationById(created.invitation.id)!!
        assertEquals(InvitationStatus.REVOKED, afterRevoke.effectiveStatus())

        // Revoked token cannot be accepted
        val rohan = AuthUser("uid_rohan", "Rohan", "rohan@example.com")
        val revokedValidation = repoB.validateInvitationToken(created.shareLink, rohan)
        assertTrue(revokedValidation is InvitationValidationResult.Revoked)
        val revokedAccept = repoB.acceptInvitation(created.shareLink, rohan)
        assertTrue(revokedAccept is InvitationAcceptResult.Error)

        // Expired token cannot be accepted
        val expiredRawToken = InvitationTokenUtil.generateSecureToken()
        val expiredHash = InvitationTokenUtil.sha256Hex(expiredRawToken)
        val expiredEntity = InvitationEntity(
            id = "inv_expired_1",
            householdId = householdId,
            householdName = "Sharma Family",
            inviterUserId = ownerUser.uid,
            inviterName = ownerUser.displayName,
            inviteeEmail = "",
            invitationTokenHash = expiredHash,
            role = HouseholdRole.MEMBER.value,
            status = InvitationStatus.PENDING.value,
            createdAt = System.currentTimeMillis() - 10_000_000L,
            expiresAt = System.currentTimeMillis() - 1_000L
        )
        sharedCloud.invitationsByHash[expiredHash] = expiredEntity

        val expiredValidation = repoB.validateInvitationToken(expiredRawToken, rohan)
        assertTrue(expiredValidation is InvitationValidationResult.Expired)
        val expiredAccept = repoB.acceptInvitation(expiredRawToken, rohan)
        assertTrue(expiredAccept is InvitationAcceptResult.Error)
    }

    // TEST 12: RBAC prevents unauthorized member from inviting, revoking, self-escalating, or removing owner
    @Test
    fun test12_rbacBlocksUnauthorizedInviteRevokeEscalationAndOwnerRemoval() = runBlocking {
        // Add recipientUser as MEMBER
        val created = repoA.createInvitation(
            householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, recipientUser.email
        ) as InvitationCreateResult.Success
        val accept = repoB.acceptInvitation(created.shareLink, recipientUser)
        assertTrue(accept is InvitationAcceptResult.Accepted)

        val priyaMember = sharedCloud.membersByHousehold[householdId]!![recipientUser.uid]!!
        val ownerMember = sharedCloud.membersByHousehold[householdId]!![ownerUser.uid]!!
        dbA.familyMemberDao().insertMember(priyaMember)

        // Regular MEMBER cannot create invitations
        val memberInviteAttempt = repoA.createInvitation(
            householdId, "Sharma Family", recipientUser, HouseholdRole.MEMBER, "someone@example.com"
        )
        assertTrue(memberInviteAttempt is InvitationCreateResult.Error)

        // Regular MEMBER cannot revoke invitations
        val pending2 = repoA.createInvitation(
            householdId, "Sharma Family", ownerUser, HouseholdRole.OWNER, "grandma@example.com"
        ) as InvitationCreateResult.Success
        val memberRevokeAttempt = repoA.revokeInvitation(pending2.invitation.id, recipientUser, HouseholdRole.MEMBER)
        assertTrue(memberRevokeAttempt is InvitationActionResult.Error)

        // User cannot escalate their own role to ADMIN or OWNER
        val selfEscalate = repoA.updateMemberRole(
            householdId, priyaMember, HouseholdRole.ADMIN, recipientUser.uid, HouseholdRole.MEMBER
        )
        assertTrue(selfEscalate is MemberManagementResult.Error)
        val selfOwnerEscalate = repoA.updateMemberRole(
            householdId, priyaMember, HouseholdRole.OWNER, recipientUser.uid, HouseholdRole.ADMIN
        )
        assertTrue(selfOwnerEscalate is MemberManagementResult.Error)

        // Owner CAN promote MEMBER -> ADMIN
        val promoteRes = repoA.updateMemberRole(
            householdId, priyaMember, HouseholdRole.ADMIN, ownerUser.uid, HouseholdRole.OWNER
        )
        assertTrue(promoteRes is MemberManagementResult.Success)

        // Even ADMIN cannot remove the OWNER
        val removeOwnerAttempt = repoA.removeMember(
            householdId, ownerMember, recipientUser.uid, HouseholdRole.ADMIN
        )
        assertTrue(removeOwnerAttempt is MemberManagementResult.Error)
    }

    // TEST 14: Existing expense, budget, and analytics repository functionality remains completely intact
    @Test
    fun test14_existingExpenseAndBudgetFeaturesRemainIntact() = runBlocking {
        val expenseRepo = ExpenseRepository(
            dbA.expenseDao(),
            dbA.familyMemberDao(),
            dbA.categoryBudgetDao(),
            dbA.householdDao(),
            dbA.incomeDao(),
            dbA.invitationDao()
        )
        val expense = ExpenseEntity(
            amount = 450.0,
            currencySymbol = "₹",
            category = "Groceries",
            description = "Organic Produce",
            paidByMemberId = ownerUser.uid,
            paidByMemberName = ownerUser.displayName,
            splitType = "Split Equally",
            timestamp = System.currentTimeMillis(),
            householdId = householdId
        )
        expenseRepo.addExpense(expense)
        expenseRepo.setCategoryBudget(CategoryBudgetEntity("Groceries", 2000.0, householdId))

        val savedExpenses = expenseRepo.getExpenses(householdId).first()
        val savedBudgets = expenseRepo.getBudgets(householdId).first()
        assertEquals(1, savedExpenses.size)
        assertEquals("Organic Produce", savedExpenses.first().description)
        assertEquals(1, savedBudgets.size)
        assertEquals(2000.0, savedBudgets.first().monthlyLimit, 0.01)
    }
}
