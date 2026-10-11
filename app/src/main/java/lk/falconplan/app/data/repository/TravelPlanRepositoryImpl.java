package lk.falconplan.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.PublicTravelPlan;
import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;
import lk.falconplan.app.data.remote.BackendApiClient;

public class TravelPlanRepositoryImpl implements TravelPlanRepository {

    public static final String USERS_COLLECTION = "users";
    public static final String TRAVEL_PLANS_COLLECTION = "travelPlans";
    public static final String STOPS_COLLECTION = "stops";
    public static final String PUBLIC_TRAVEL_PLANS_COLLECTION = "publicTravelPlans";

    private final FirebaseFirestore db;
    private final BackendApiClient apiClient;

    public TravelPlanRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
        this.apiClient = BackendApiClient.getInstance();
    }

    public TravelPlanRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
        this.apiClient = BackendApiClient.getInstance();
    }

    @Override
    public void createPlan(String uid, TravelPlan plan, OperationCallback callback) {
        if (uid == null || uid.isEmpty() || plan == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid user or plan data"));
            return;
        }

        if (!plan.isValidDateRange()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Start date cannot be after end date"));
            return;
        }

        DocumentReference docRef = db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document();

        String planId = docRef.getId();
        plan.setId(planId);
        plan.setOwnerUid(uid);

        docRef.set(plan.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getUserPlans(String uid, PlansCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("UID cannot be empty"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TravelPlan> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TravelPlan plan = doc.toObject(TravelPlan.class);
                        if (plan != null) {
                            list.add(plan);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getPlanDetails(String uid, String planId, PlanCallback callback) {
        if (uid == null || planId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        TravelPlan plan = doc.toObject(TravelPlan.class);
                        if (callback != null) callback.onSuccess(plan);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void updatePlan(String uid, TravelPlan plan, OperationCallback callback) {
        if (uid == null || plan == null || plan.getId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        plan.setUpdatedAt(System.currentTimeMillis());

        db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(plan.getId())
                .set(plan.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void deletePlan(String uid, String planId, OperationCallback callback) {
        if (uid == null || planId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        DocumentReference planRef = db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId);

        planRef.collection(STOPS_COLLECTION).get()
                .addOnSuccessListener(stopsSnap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : stopsSnap) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(planRef);
                    batch.delete(db.collection(PUBLIC_TRAVEL_PLANS_COLLECTION).document(planId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void addStopToPlan(String uid, String planId, TravelPlanStop stop, OperationCallback callback) {
        if (uid == null || planId == null || stop == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        if (!stop.isValidCoordinates()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid stop coordinates"));
            return;
        }

        DocumentReference planRef = db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId);

        DocumentReference stopRef = planRef.collection(STOPS_COLLECTION).document();
        stop.setId(stopRef.getId());

        planRef.collection(STOPS_COLLECTION).get()
                .addOnSuccessListener(stopsSnap -> {
                    int currentCount = stopsSnap.size();
                    stop.setOrderIndex(currentCount + 1);

                    WriteBatch batch = db.batch();
                    batch.set(stopRef, stop.toMap());
                    batch.update(planRef, "stopCount", currentCount + 1, "updatedAt", System.currentTimeMillis());

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getStopsForPlan(String uid, String planId, StopsCallback callback) {
        if (uid == null || planId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId)
                .collection(STOPS_COLLECTION)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TravelPlanStop> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TravelPlanStop stop = doc.toObject(TravelPlanStop.class);
                        if (stop != null) {
                            list.add(stop);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void updateStop(String uid, String planId, TravelPlanStop stop, OperationCallback callback) {
        if (uid == null || planId == null || stop == null || stop.getId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId)
                .collection(STOPS_COLLECTION).document(stop.getId())
                .set(stop.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void removeStopFromPlan(String uid, String planId, String stopId, OperationCallback callback) {
        if (uid == null || planId == null || stopId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        DocumentReference planRef = db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document(planId);

        DocumentReference stopRef = planRef.collection(STOPS_COLLECTION).document(stopId);

        stopRef.delete()
                .addOnSuccessListener(aVoid -> {
                    planRef.collection(STOPS_COLLECTION).get()
                            .addOnSuccessListener(stopsSnap -> {
                                int newCount = stopsSnap.size();
                                planRef.update("stopCount", newCount, "updatedAt", System.currentTimeMillis());
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onSuccess();
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void reorderStops(String uid, String planId, List<TravelPlanStop> stops, OperationCallback callback) {
        if (uid == null || planId == null || stops == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        WriteBatch batch = db.batch();
        for (int i = 0; i < stops.size(); i++) {
            TravelPlanStop stop = stops.get(i);
            stop.setOrderIndex(i + 1);
            DocumentReference stopRef = db.collection(USERS_COLLECTION).document(uid)
                    .collection(TRAVEL_PLANS_COLLECTION).document(planId)
                    .collection(STOPS_COLLECTION).document(stop.getId());
            batch.update(stopRef, "orderIndex", stop.getOrderIndex());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void publishPlan(String uid, TravelPlan plan, List<TravelPlanStop> stops, String ownerDisplayName, OperationCallback callback) {
        if (uid == null || plan == null || plan.getId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                apiClient.publishPlan(idToken, plan, stops, ownerDisplayName, new BackendApiClient.ApiCallback<PublicTravelPlan>() {
                    @Override
                    public void onSuccess(PublicTravelPlan result) {
                        // Update private plan status
                        db.collection(USERS_COLLECTION).document(uid)
                                .collection(TRAVEL_PLANS_COLLECTION).document(plan.getId())
                                .update("status", "published", "updatedAt", System.currentTimeMillis())
                                .addOnCompleteListener(t -> {
                                    if (callback != null) callback.onSuccess();
                                });
                    }

                    @Override
                    public void onError(Exception exception) {
                        // Direct Firestore transaction fallback if backend API fails or is unconfigured locally
                        long now = System.currentTimeMillis();
                        PublicTravelPlan publicPlan = new PublicTravelPlan(
                                plan.getId(),
                                uid,
                                ownerDisplayName != null ? ownerDisplayName : "FalconPlan Member",
                                plan.getTitle(),
                                plan.getDescription(),
                                plan.getStartDate(),
                                plan.getEndDate(),
                                now,
                                "published",
                                stops != null ? stops : new ArrayList<>()
                        );

                        WriteBatch batch = db.batch();
                        DocumentReference publicRef = db.collection(PUBLIC_TRAVEL_PLANS_COLLECTION).document(plan.getId());
                        DocumentReference privateRef = db.collection(USERS_COLLECTION).document(uid)
                                .collection(TRAVEL_PLANS_COLLECTION).document(plan.getId());

                        batch.set(publicRef, publicPlan.toMap());
                        batch.update(privateRef, "status", "published", "updatedAt", now);

                        batch.commit()
                                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                                .addOnFailureListener(e -> { if (callback != null) callback.onError(e); });
                    }
                });
            });
        }
    }

    @Override
    public void unpublishPlan(String uid, String planId, OperationCallback callback) {
        if (uid == null || planId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                apiClient.unpublishPlan(idToken, planId, new BackendApiClient.ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        db.collection(USERS_COLLECTION).document(uid)
                                .collection(TRAVEL_PLANS_COLLECTION).document(planId)
                                .update("status", TravelPlan.STATUS_DRAFT, "updatedAt", System.currentTimeMillis())
                                .addOnCompleteListener(t -> {
                                    if (callback != null) callback.onSuccess();
                                });
                    }

                    @Override
                    public void onError(Exception exception) {
                        // Direct Firestore transaction fallback
                        long now = System.currentTimeMillis();
                        WriteBatch batch = db.batch();
                        DocumentReference publicRef = db.collection(PUBLIC_TRAVEL_PLANS_COLLECTION).document(planId);
                        DocumentReference privateRef = db.collection(USERS_COLLECTION).document(uid)
                                .collection(TRAVEL_PLANS_COLLECTION).document(planId);

                        batch.delete(publicRef);
                        batch.update(privateRef, "status", TravelPlan.STATUS_DRAFT, "updatedAt", now);

                        batch.commit()
                                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                                .addOnFailureListener(e -> { if (callback != null) callback.onError(e); });
                    }
                });
            });
        }
    }

    @Override
    public void getPublicPlans(PublicPlansCallback callback) {
        apiClient.getPublicPlans(1, 20, new BackendApiClient.ApiCallback<List<PublicTravelPlan>>() {
            @Override
            public void onSuccess(List<PublicTravelPlan> result) {
                if (result != null && !result.isEmpty()) {
                    if (callback != null) callback.onSuccess(result);
                } else {
                    fetchPublicPlansFromFirestore(callback);
                }
            }

            @Override
            public void onError(Exception exception) {
                fetchPublicPlansFromFirestore(callback);
            }
        });
    }

    private void fetchPublicPlansFromFirestore(PublicPlansCallback callback) {
        db.collection(PUBLIC_TRAVEL_PLANS_COLLECTION)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PublicTravelPlan> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        PublicTravelPlan plan = doc.toObject(PublicTravelPlan.class);
                        if (plan != null) {
                            list.add(plan);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void copyPublicPlanToPrivate(String uid, PublicTravelPlan publicPlan, OperationCallback callback) {
        if (uid == null || publicPlan == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        long now = System.currentTimeMillis();
        DocumentReference planRef = db.collection(USERS_COLLECTION).document(uid)
                .collection(TRAVEL_PLANS_COLLECTION).document();

        String newPlanId = planRef.getId();
        int stopCount = publicPlan.getStops() != null ? publicPlan.getStops().size() : 0;

        TravelPlan newPrivatePlan = new TravelPlan(
                newPlanId,
                uid,
                "Copy of " + publicPlan.getTitle(),
                publicPlan.getDescription(),
                publicPlan.getStartDate(),
                publicPlan.getEndDate(),
                TravelPlan.STATUS_DRAFT,
                now,
                now,
                stopCount
        );

        WriteBatch batch = db.batch();
        batch.set(planRef, newPrivatePlan.toMap());

        if (publicPlan.getStops() != null) {
            for (int i = 0; i < publicPlan.getStops().size(); i++) {
                TravelPlanStop origStop = publicPlan.getStops().get(i);
                DocumentReference stopRef = planRef.collection(STOPS_COLLECTION).document();
                TravelPlanStop copiedStop = new TravelPlanStop(
                        stopRef.getId(),
                        origStop.getDestinationId(),
                        origStop.getDestinationName(),
                        origStop.getDistrict(),
                        origStop.getLatitude(),
                        origStop.getLongitude(),
                        origStop.getVisitDate(),
                        "", // Clear original private notes
                        i + 1
                );
                batch.set(stopRef, copiedStop.toMap());
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}