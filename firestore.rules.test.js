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

  test("User can create their own profile with valid activeMode (traveler/provider)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice").set({
      uid: "user_alice",
      displayName: "Alice Silva",
      email: "alice@example.com",
      activeMode: "traveler",
      createdAt: Date.now(),
      updatedAt: Date.now()
    }));
  });

  test("User CANNOT create profile with privileged roles (isAdmin/isVerified)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice").set({
      uid: "user_alice",
      displayName: "Alice Silva",
      activeMode: "traveler",
      isAdmin: true
    }));
  });

  test("Unauthenticated user CANNOT access any documents", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.doc("users/user_alice").get());
    await assertFails(anonDb.collection("homestays").get());
  });
});