package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;

public interface TravelPlanRepository {

    interface PlansCallback {
        void onSuccess(List<TravelPlan> plans);
        void onError(Exception exception);
    }

    interface PlanCallback {
        void onSuccess(TravelPlan plan);
        void onError(Exception exception);
    }

    interface StopsCallback {
        void onSuccess(List<TravelPlanStop> stops);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void createPlan(String uid, TravelPlan plan, OperationCallback callback);

    void getUserPlans(String uid, PlansCallback callback);

    void getPlanDetails(String uid, String planId, PlanCallback callback);

    void updatePlan(String uid, TravelPlan plan, OperationCallback callback);

    void deletePlan(String uid, String planId, OperationCallback callback);

    void addStopToPlan(String uid, String planId, TravelPlanStop stop, OperationCallback callback);

    void getStopsForPlan(String uid, String planId, StopsCallback callback);

    void updateStop(String uid, String planId, TravelPlanStop stop, OperationCallback callback);

    void removeStopFromPlan(String uid, String planId, String stopId, OperationCallback callback);

    void reorderStops(String uid, String planId, List<TravelPlanStop> stops, OperationCallback callback);
}