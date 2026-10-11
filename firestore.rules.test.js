/**
 * Security Rules Contract Tests for FalconPlan (Profile + SavedPlaces + TravelPlans)
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

  test("User can create a private TravelPlan", async () => {
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

  test("User CANNOT access another user's TravelPlan", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("users/user_bob/travelPlans/plan_001").get());
  });

  test("User can add a stop to their TravelPlan", async () => {
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

  test("Unauthenticated user CANNOT read or write any documents", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.doc("users/user_alice").get());
    await assertFails(anonDb.doc("users/user_alice/travelPlans/plan_001").get());
  });
});