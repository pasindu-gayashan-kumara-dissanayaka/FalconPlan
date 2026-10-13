/**
 * Security Rules Contract Tests for FalconPlan (Profile + SavedPlaces + TravelPlans + ProviderProfiles + KYC)
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

  test("User can create ProviderProfile with valid schema and kycStatus == pending_identity_verification", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertSucceeds(aliceDb.doc("providerProfiles/user_alice").set({
      uid: "user_alice",
      hostDisplayName: "Saman Perera",
      businessName: "Knuckles Eco Homestay",
      businessType: "homestay",
      district: "Matale",
      village: "Meemure",
      shortDescription: "Authentic village stay",
      contactPhone: "+94 77 123 4567",
      kycStatus: "pending_identity_verification",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User CANNOT create ProviderProfile directly with kycStatus == verified (privilege escalation)", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("providerProfiles/user_alice").set({
      uid: "user_alice",
      hostDisplayName: "Saman Perera",
      businessName: "Knuckles Eco Homestay",
      businessType: "homestay",
      district: "Matale",
      village: "Meemure",
      shortDescription: "Authentic village stay",
      contactPhone: "+94 77 123 4567",
      kycStatus: "verified", // Privilege escalation attempt DENIED!
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User CANNOT update ProviderProfile kycStatus to verified client-side", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("providerProfiles/user_alice").set({
        uid: "user_alice",
        hostDisplayName: "Saman Perera",
        businessName: "Knuckles Eco Homestay",
        businessType: "homestay",
        district: "Matale",
        village: "Meemure",
        shortDescription: "Authentic village stay",
        contactPhone: "+94 77 123 4567",
        kycStatus: "pending_identity_verification",
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("providerProfiles/user_alice").update({
      kycStatus: "verified" // Privilege escalation attempt DENIED!
    }));
  });

  test("Direct client-side read and write to kyc/{userId} is DENIED", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("kyc/user_alice").get());
    await assertFails(aliceDb.doc("kyc/user_alice").set({
      providerUid: "user_alice",
      status: "verified"
    }));
  });

  test("User CANNOT create ProviderProfile with mismatched UID", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("providerProfiles/user_alice").set({
      uid: "user_bob", // Mismatch
      hostDisplayName: "Saman Perera",
      businessName: "Knuckles Eco Homestay",
      businessType: "homestay",
      district: "Matale",
      village: "Meemure",
      shortDescription: "Authentic village stay",
      contactPhone: "+94 77 123 4567",
      kycStatus: "pending_identity_verification",
      createdAt: 1700000000000,
      updatedAt: 1700000000000
    }));
  });

  test("User CANNOT read another user's ProviderProfile", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("providerProfiles/user_bob").get());
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

  test("Direct client-side read and write to publicTravelPlans is DENIED", async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.doc("publicTravelPlans/plan_pub_1").get());
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