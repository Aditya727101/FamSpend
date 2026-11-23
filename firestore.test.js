const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const HASH_VALID = "a".repeat(64);
const HASH_EXPIRED = "b".repeat(64);
const HASH_REVOKED = "c".repeat(64);
const HASH_ADMIN_INVITE = "d".repeat(64);

let testEnv;

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function dbFor(uid, extraToken = {}) {
  if (!uid) {
    return testEnv.unauthenticatedContext().firestore();
  }
  return testEnv.authenticatedContext(uid, extraToken).firestore();
}

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function futureDate(offsetMs = 7 * 24 * 60 * 60 * 1000) {
  return new Date(Date.now() + offsetMs);
}

async function seedHouseholdWithOwner(ownerUid = "owner_1", householdId = "FAM-OWNER1") {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const created = pastDate(10000);
    await db.collection("households").doc(householdId).set({
      householdId,
      householdName: "Sharma Family",
      inviteCode: householdId,
      defaultCurrency: "₹",
      totalMonthlyBudget: 5000,
      lastSyncedTimestamp: Date.now(),
      isLiveSyncEnabled: true,
      ownerUserId: ownerUid,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("households").doc(householdId).collection("members").doc(ownerUid).set({
      id: ownerUid,
      userId: ownerUid,
      name: "Aarav",
      email: "aarav@example.com",
      role: "Primary",
      householdRole: "OWNER",
      membershipStatus: "ACTIVE",
      avatarColorHex: "#4F46E5",
      avatarIcon: "person",
      householdId,
      monthlyContributionGoal: 0,
      joinedAt: created,
      updatedAt: created,
    });
    await db.collection("households").doc(householdId).collection("expenses").doc("exp_1").set({
      id: "exp_1",
      amount: 1200,
      category: "Groceries",
      description: "Weekly supermarket run",
      householdId,
      timestamp: Date.now(),
    });
  });
}

async function seedMember(householdId, memberUid, role = "MEMBER", email = "member@example.com") {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const created = pastDate(8000);
    await db.collection("households").doc(householdId).collection("members").doc(memberUid).set({
      id: memberUid,
      userId: memberUid,
      name: "Priya",
      email,
      role: role === "ADMIN" ? "Admin" : "Member",
      householdRole: role,
      membershipStatus: "ACTIVE",
      avatarColorHex: "#10B981",
      avatarIcon: "person",
      householdId,
      monthlyContributionGoal: 0,
      joinedAt: created,
      updatedAt: created,
    });
  });
}

function buildPendingInvitationPayload({
  tokenHash,
  householdId = "FAM-OWNER1",
  inviterUserId = "owner_1",
  inviteeEmail = "priya@example.com",
  role = "MEMBER",
  adminUids = ["owner_1"],
  expiresAt = futureDate(),
}) {
  const now = pastDate(1500);
  return {
    id: `inv_${tokenHash.slice(0, 8)}`,
    householdId,
    householdName: "Sharma Family",
    inviterUserId,
    inviterName: "Aarav",
    inviteeEmail,
    inviteeName: "Priya",
    personalMessage: "Welcome to our shared budget!",
    invitationTokenHash: tokenHash,
    role,
    status: "PENDING",
    adminUids,
    createdAt: now,
    expiresAt,
    acceptedAt: null,
    acceptedByUserId: "",
    revokedAt: null,
    updatedAt: now,
  };
}

test("1. Unauthenticated users cannot read or write private household data or invitations", async () => {
  await seedHouseholdWithOwner();
  const anonDb = dbFor(null);
  await assertFails(anonDb.collection("households").doc("FAM-OWNER1").get());
  await assertFails(anonDb.collection("households").doc("FAM-OWNER1").collection("expenses").doc("exp_1").get());
  await assertFails(anonDb.collection("households").doc("FAM-OWNER1").collection("members").doc("owner_1").get());
  await assertFails(anonDb.collection("invitations").doc(HASH_VALID).get());
});

test("2. Non-member cannot read private household expenses or members before accepting an invitation", async () => {
  await seedHouseholdWithOwner();
  const outsiderDb = dbFor("outsider_1", { email: "priya@example.com", email_verified: true });
  await assertFails(outsiderDb.collection("households").doc("FAM-OWNER1").get());
  await assertFails(outsiderDb.collection("households").doc("FAM-OWNER1").collection("expenses").doc("exp_1").get());
  await assertFails(outsiderDb.collection("households").doc("FAM-OWNER1").collection("expenses").get());
  await assertFails(outsiderDb.collection("households").doc("FAM-OWNER1").collection("members").get());
});

test("3. Owner can bootstrap a new household atomically with OWNER membership", async () => {
  const ownerDb = dbFor("owner_new", { email: "owner@example.com", email_verified: true });
  const now = pastDate(1500);
  await assertSucceeds(
    ownerDb.runTransaction(async (tx) => {
      tx.set(ownerDb.collection("households").doc("FAM-NEW1"), {
        householdId: "FAM-NEW1",
        householdName: "New Family",
        inviteCode: "FAM-NEW1",
        defaultCurrency: "₹",
        totalMonthlyBudget: 4000,
        lastSyncedTimestamp: Date.now(),
        isLiveSyncEnabled: true,
        ownerUserId: "owner_new",
        createdAt: now,
        updatedAt: now,
      });
      tx.set(ownerDb.collection("households").doc("FAM-NEW1").collection("members").doc("owner_new"), {
        id: "owner_new",
        userId: "owner_new",
        name: "Owner",
        email: "owner@example.com",
        role: "Primary",
        householdRole: "OWNER",
        membershipStatus: "ACTIVE",
        avatarColorHex: "#4F46E5",
        avatarIcon: "person",
        householdId: "FAM-NEW1",
        monthlyContributionGoal: 0,
        joinedAt: now,
        updatedAt: now,
      });
      tx.set(ownerDb.collection("users").doc("owner_new"), {
        uid: "owner_new",
        displayName: "Owner",
        email: "owner@example.com",
        activeHouseholdId: "FAM-NEW1",
        updatedAt: now,
      });
    })
  );
});

test("4. Owner can create invitation; regular MEMBER cannot create invitations", async () => {
  await seedHouseholdWithOwner();
  await seedMember("FAM-OWNER1", "member_1", "MEMBER", "member1@example.com");

  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_VALID).set(
      buildPendingInvitationPayload({ tokenHash: HASH_VALID })
    )
  );

  const memberDb = dbFor("member_1", { email: "member1@example.com", email_verified: true });
  await assertFails(
    memberDb.collection("invitations").doc(HASH_ADMIN_INVITE).set(
      buildPendingInvitationPayload({
        tokenHash: HASH_ADMIN_INVITE,
        inviterUserId: "member_1",
        adminUids: ["owner_1", "member_1"],
      })
    )
  );
});

test("5. Authorized Owner can list household invitations; outsider cannot enumerate invitations", async () => {
  await seedHouseholdWithOwner();
  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_VALID).set(
      buildPendingInvitationPayload({ tokenHash: HASH_VALID })
    )
  );

  await assertSucceeds(
    ownerDb
      .collection("invitations")
      .where("householdId", "==", "FAM-OWNER1")
      .where("adminUids", "array-contains", "owner_1")
      .get()
  );

  const outsiderDb = dbFor("outsider_2", { email: "stranger@example.com", email_verified: true });
  await assertFails(outsiderDb.collection("invitations").get());
});

test("6. Recipient accepts valid single-use invitation atomically and gains household access; reuse is blocked", async () => {
  await seedHouseholdWithOwner();
  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_VALID).set(
      buildPendingInvitationPayload({ tokenHash: HASH_VALID, inviteeEmail: "priya@example.com" })
    )
  );

  const recipientDb = dbFor("user_priya", { email: "priya@example.com", email_verified: true });
  await assertSucceeds(recipientDb.collection("invitations").doc(HASH_VALID).get());

  const acceptTime = pastDate(1000);
  await assertSucceeds(
    recipientDb.runTransaction(async (tx) => {
      tx.set(recipientDb.collection("households").doc("FAM-OWNER1").collection("members").doc("user_priya"), {
        id: "user_priya",
        userId: "user_priya",
        name: "Priya",
        email: "priya@example.com",
        role: "Member",
        householdRole: "MEMBER",
        membershipStatus: "ACTIVE",
        avatarColorHex: "#10B981",
        avatarIcon: "person",
        householdId: "FAM-OWNER1",
        monthlyContributionGoal: 0,
        acceptedInvitationHash: HASH_VALID,
        joinedAt: acceptTime,
        updatedAt: acceptTime,
      });
      tx.update(recipientDb.collection("invitations").doc(HASH_VALID), {
        status: "ACCEPTED",
        acceptedAt: acceptTime,
        acceptedByUserId: "user_priya",
        updatedAt: acceptTime,
      });
      tx.set(recipientDb.collection("users").doc("user_priya"), {
        uid: "user_priya",
        displayName: "Priya",
        email: "priya@example.com",
        activeHouseholdId: "FAM-OWNER1",
        updatedAt: acceptTime,
      });
    })
  );

  // Accepted member can now read household expenses
  await assertSucceeds(
    recipientDb.collection("households").doc("FAM-OWNER1").collection("expenses").doc("exp_1").get()
  );

  // Reuse attempt by another user MUST FAIL
  const secondUserDb = dbFor("user_reuse", { email: "priya@example.com", email_verified: true });
  await assertFails(
    secondUserDb.runTransaction(async (tx) => {
      tx.set(secondUserDb.collection("households").doc("FAM-OWNER1").collection("members").doc("user_reuse"), {
        id: "user_reuse",
        userId: "user_reuse",
        name: "Reuse",
        email: "priya@example.com",
        role: "Member",
        householdRole: "MEMBER",
        membershipStatus: "ACTIVE",
        avatarColorHex: "#10B981",
        avatarIcon: "person",
        householdId: "FAM-OWNER1",
        monthlyContributionGoal: 0,
        acceptedInvitationHash: HASH_VALID,
        joinedAt: acceptTime,
        updatedAt: acceptTime,
      });
      tx.update(secondUserDb.collection("invitations").doc(HASH_VALID), {
        status: "ACCEPTED",
        acceptedAt: acceptTime,
        acceptedByUserId: "user_reuse",
        updatedAt: acceptTime,
      });
    })
  );
});

test("7. Wrong email cannot accept email-specific invitation; role escalation on accept is rejected", async () => {
  await seedHouseholdWithOwner();
  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_VALID).set(
      buildPendingInvitationPayload({ tokenHash: HASH_VALID, inviteeEmail: "priya@example.com", role: "MEMBER" })
    )
  );

  const acceptTime = pastDate(1000);
  const wrongEmailDb = dbFor("user_wrong", { email: "wrong@example.com", email_verified: true });
  await assertFails(
    wrongEmailDb.runTransaction(async (tx) => {
      tx.set(wrongEmailDb.collection("households").doc("FAM-OWNER1").collection("members").doc("user_wrong"), {
        id: "user_wrong",
        userId: "user_wrong",
        name: "Wrong",
        email: "wrong@example.com",
        role: "Member",
        householdRole: "MEMBER",
        membershipStatus: "ACTIVE",
        avatarColorHex: "#10B981",
        avatarIcon: "person",
        householdId: "FAM-OWNER1",
        monthlyContributionGoal: 0,
        acceptedInvitationHash: HASH_VALID,
        joinedAt: acceptTime,
        updatedAt: acceptTime,
      });
      tx.update(wrongEmailDb.collection("invitations").doc(HASH_VALID), {
        status: "ACCEPTED",
        acceptedAt: acceptTime,
        acceptedByUserId: "user_wrong",
        updatedAt: acceptTime,
      });
    })
  );

  const priyaDb = dbFor("user_priya", { email: "priya@example.com", email_verified: true });
  await assertFails(
    priyaDb.runTransaction(async (tx) => {
      tx.set(priyaDb.collection("households").doc("FAM-OWNER1").collection("members").doc("user_priya"), {
        id: "user_priya",
        userId: "user_priya",
        name: "Priya",
        email: "priya@example.com",
        role: "Admin",
        householdRole: "ADMIN",
        membershipStatus: "ACTIVE",
        avatarColorHex: "#10B981",
        avatarIcon: "person",
        householdId: "FAM-OWNER1",
        monthlyContributionGoal: 0,
        acceptedInvitationHash: HASH_VALID,
        joinedAt: acceptTime,
        updatedAt: acceptTime,
      });
      tx.update(priyaDb.collection("invitations").doc(HASH_VALID), {
        status: "ACCEPTED",
        acceptedAt: acceptTime,
        acceptedByUserId: "user_priya",
        updatedAt: acceptTime,
      });
    })
  );
});

test("8. Expired and revoked invitations cannot be accepted; unauthorized member cannot revoke", async () => {
  await seedHouseholdWithOwner();
  await seedMember("FAM-OWNER1", "member_1", "MEMBER", "member1@example.com");

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("invitations").doc(HASH_EXPIRED).set({
      ...buildPendingInvitationPayload({
        tokenHash: HASH_EXPIRED,
        inviteeEmail: "",
        expiresAt: pastDate(60 * 60 * 1000),
      }),
      createdAt: pastDate(2 * 60 * 60 * 1000),
      updatedAt: pastDate(2 * 60 * 60 * 1000),
    });
  });

  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_REVOKED).set(
      buildPendingInvitationPayload({ tokenHash: HASH_REVOKED, inviteeEmail: "" })
    )
  );

  const now = pastDate(1000);
  const memberDb = dbFor("member_1", { email: "member1@example.com", email_verified: true });
  await assertFails(
    memberDb.collection("invitations").doc(HASH_REVOKED).update({
      status: "REVOKED",
      revokedAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    ownerDb.collection("invitations").doc(HASH_REVOKED).update({
      status: "REVOKED",
      revokedAt: now,
      updatedAt: now,
    })
  );

  const joinerDb = dbFor("user_joiner", { email: "joiner@example.com", email_verified: true });
  for (const hash of [HASH_EXPIRED, HASH_REVOKED]) {
    await assertFails(
      joinerDb.runTransaction(async (tx) => {
        tx.set(joinerDb.collection("households").doc("FAM-OWNER1").collection("members").doc("user_joiner"), {
          id: "user_joiner",
          userId: "user_joiner",
          name: "Joiner",
          email: "joiner@example.com",
          role: "Member",
          householdRole: "MEMBER",
          membershipStatus: "ACTIVE",
          avatarColorHex: "#10B981",
          avatarIcon: "person",
          householdId: "FAM-OWNER1",
          monthlyContributionGoal: 0,
          acceptedInvitationHash: hash,
          joinedAt: now,
          updatedAt: now,
        });
        tx.update(joinerDb.collection("invitations").doc(hash), {
          status: "ACCEPTED",
          acceptedAt: now,
          acceptedByUserId: "user_joiner",
          updatedAt: now,
        });
      })
    );
  }
});

test("9. Members cannot self-escalate roles or switch to unauthorized households; Owner can promote/demote", async () => {
  await seedHouseholdWithOwner("owner_1", "FAM-OWNER1");
  await seedHouseholdWithOwner("owner_2", "FAM-OTHER2");
  await seedMember("FAM-OWNER1", "member_1", "MEMBER", "member1@example.com");

  const now = pastDate(1000);
  const memberDb = dbFor("member_1", { email: "member1@example.com", email_verified: true });

  await assertFails(
    memberDb.collection("households").doc("FAM-OWNER1").collection("members").doc("member_1").update({
      householdRole: "ADMIN",
      updatedAt: now,
    })
  );
  await assertFails(
    memberDb.collection("households").doc("FAM-OWNER1").collection("members").doc("member_1").update({
      householdRole: "OWNER",
      updatedAt: now,
    })
  );

  await assertFails(
    memberDb.collection("users").doc("member_1").set({
      uid: "member_1",
      displayName: "Priya",
      email: "member1@example.com",
      activeHouseholdId: "FAM-OTHER2",
      updatedAt: now,
    })
  );

  const ownerDb = dbFor("owner_1", { email: "aarav@example.com", email_verified: true });
  await assertSucceeds(
    ownerDb.collection("households").doc("FAM-OWNER1").collection("members").doc("member_1").update({
      householdRole: "ADMIN",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    memberDb.collection("invitations").doc(HASH_ADMIN_INVITE).set(
      buildPendingInvitationPayload({
        tokenHash: HASH_ADMIN_INVITE,
        inviterUserId: "member_1",
        adminUids: ["owner_1", "member_1"],
      })
    )
  );

  await assertFails(
    memberDb.collection("households").doc("FAM-OWNER1").collection("members").doc("owner_1").delete()
  );
});
