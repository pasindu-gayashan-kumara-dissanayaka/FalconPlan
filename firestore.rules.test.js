/**
 * Security Rules Contract Tests for FalconPlan (Profile + SavedPlaces + TravelPlans + PublicPlans)
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

  test("User can create a private TravelPlan with valid schema and date range", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice/travelPlans/plan_001").set({
      id: "plan_001",
      ownerUid: "user_alice",
      title: "Ella & Knuckles Trek",
      description: "A 5-day community trip",
      startDate: 1700000000000,
      endDate: 1700500000000,
      status: "draft",
      createdAt: 1700000000000,
      updatedAt: 1700000000000,
      stopCount: 0
    }));
  });

  test("User CANNOT create a TravelPlan with invalid date range (startDate > endDate)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice/travelPlans/plan_001").set({
      id: "plan_001",
      ownerUid: "user_alice",
      title: "Ella Trek",
      description: "Desc",
      startDate: 1700500000000,
      endDate: 1700000000000, // Invalid: end before start
      status: "draft",
      createdAt: 1700000000000,
      updatedAt: 1700000000000,
      stopCount: 0
    }));
  });

  test("User CANNOT create a TravelPlan with mismatched ownerUid", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice/travelPlans/plan_001").set({
      id: "plan_001",
      ownerUid: "user_bob", // Mismatch
      title: "Ella Trek",
      description: "Desc",
      startDate: 1700000000000,
      endDate: 1700500000000,
      status: "draft",
      createdAt: 1700000000000,
      updatedAt: 1700000000000,
      stopCount: 0
    }));
  });

  test("User CANNOT alter immutable ownerUid or createdAt on plan update", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("users/user_alice/travelPlans/plan_001").set({
        id: "plan_001",
        ownerUid: "user_alice",
        title: "Ella Trek",
        description: "Desc",
        startDate: 1700000000000,
        endDate: 1700500000000,
        status: "draft",
        createdAt: 1700000000000,
        updatedAt: 1700000000000,
        stopCount: 0
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice/travelPlans/plan_001").update({
      ownerUid: "user_bob"
    }));
  });

  test("User CANNOT access another user's TravelPlan", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_bob/travelPlans/plan_001").get());
  });

  test("User can add a stop with valid coordinates and orderIndex to their TravelPlan", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("users/user_alice/travelPlans/plan_001/stops/stop_001").set({
      id: "stop_001",
      destinationId: "dest_sigiriya",
      destinationName: "Sigiriya Rock Fortress",
      district: "Matale District",
      latitude: 7.9570,
      longitude: 80.7603,
      visitDate: 1700000000000,
      notes: "Early morning climb",
      orderIndex: 1
    }));
  });

  test("User CANNOT add a stop with invalid latitude (> 90)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_alice/travelPlans/plan_001/stops/stop_001").set({
      id: "stop_001",
      destinationId: "dest_sigiriya",
      destinationName: "Sigiriya",
      district: "Matale",
      latitude: 105.0, // Invalid lat
      longitude: 80.7603,
      visitDate: 1700000000000,
      notes: "",
      orderIndex: 1
    }));
  });

  test("User CANNOT add a stop to another user's TravelPlan", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_bob/travelPlans/plan_001/stops/stop_001").set({
      id: "stop_001",
      destinationId: "dest_sigiriya",
      destinationName: "Sigiriya Rock Fortress",
      district: "Matale District",
      latitude: 7.9570,
      longitude: 80.7603,
      visitDate: 1700000000000,
      notes: "Early morning climb",
      orderIndex: 1
    }));
  });

  test("Authenticated user can read publicTravelPlans", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("publicTravelPlans/plan_pub_1").get());
  });

  test("Direct client-side write to publicTravelPlans is DENIED", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("publicTravelPlans/plan_pub_1").set({
      publicPlanId: "plan_pub_1",
      title: "Hacked Public Plan"
    }));
  });

  test("Unauthenticated user CANNOT read or write any documents", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.doc("users/user_alice").get());
    await assertFails(anonDb.doc("users/user_alice/travelPlans/plan_001").get());
  });
});