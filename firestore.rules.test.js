/**
 * Security Rules Contract Tests for FalconPlan
 * Tested with @firebase/rules-unit-testing
 */

const { assertFails, assertSucceeds, initializeTestEnvironment } = require('@firebase/rules-unit-testing');
const fs = require('fs');

describe("FalconPlan Firestore Security Rules", () => {
  let testEnv;

  beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: "falconplan-88ad9",
      firestore: {
        rules: fs.readFileSync("firestore.rules", "utf8"),
      },
    });
  });

  afterAll(async () => {
    await testEnv.cleanup();
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  test("User can read their own profile", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice").get());
  });

  test("User CANNOT read another user's profile", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_bob").get());
  });

  test("User can create their own profile with valid schema (traveler mode)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice").set({
      uid: "user_alice",
      displayName: "Alice Silva",
      email: "alice@example.com",
      photoUrl: "",
      activeMode: "traveler",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User can create their own profile with valid schema (provider mode)", async () => {
    const bobDb = testEnv.authenticatedContext("user_bob").firestore();
    await assertSucceeds(bobDb.doc("users/user_bob").set({
      uid: "user_bob",
      displayName: "Bob Host",
      email: "bob@example.com",
      photoUrl: "",
      activeMode: "provider",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User CANNOT create profile with unapproved fields (isAdmin/isVerified/kycStatus)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").set({
      uid: "user_alice",
      displayName: "Alice Silva",
      email: "alice@example.com",
      photoUrl: "",
      activeMode: "traveler",
      createdAt: 1700000000000,
      updatedAt: 1700000000000,
      isAdmin: true
    }));
  });

  test("User CANNOT create profile with invalid activeMode (e.g. admin)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").set({
      uid: "user_alice",
      displayName: "Alice Silva",
      email: "alice@example.com",
      photoUrl: "",
      activeMode: "admin",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User CANNOT create profile with mismatched UID in document", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").set({
      uid: "user_bob",
      displayName: "Alice Silva",
      email: "alice@example.com",
      photoUrl: "",
      activeMode: "traveler",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User can update activeMode and updatedAt in their own profile", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("users/user_alice").set({
        uid: "user_alice",
        displayName: "Alice Silva",
        email: "alice@example.com",
        photoUrl: "",
        activeMode: "traveler",
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice").update({
      activeMode: "provider",
      updatedAt: 1700000005000
    }));
  });

  test("User CANNOT alter immutable uid or createdAt on profile update", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("users/user_alice").set({
        uid: "user_alice",
        displayName: "Alice Silva",
        email: "alice@example.com",
        photoUrl: "",
        activeMode: "traveler",
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").update({
      createdAt: 1800000000000
    }));
  });

  test("User CANNOT inject privileged keys on profile update", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("users/user_alice").set({
        uid: "user_alice",
        displayName: "Alice Silva",
        email: "alice@example.com",
        photoUrl: "",
        activeMode: "traveler",
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").update({
      isVerifiedProvider: true
    }));
  });

  test("Unauthenticated user CANNOT access any documents", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.doc("users/user_alice").get());
    await assertFails(anonDb.collection("homestays").get());
  });
});