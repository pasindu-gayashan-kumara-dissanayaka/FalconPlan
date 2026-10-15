package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.model.BookingQuote;

public class BookingRepositoryImpl implements BookingRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public BookingRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void getPublicRooms(String listingId, PublicRoomsCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "public-listings/" + listingId + "/rooms");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    JSONArray rooms = data != null ? data.optJSONArray("rooms") : new JSONArray();

                    mainHandler.post(() -> callback.onSuccess(rooms != null ? rooms : new JSONArray()));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getBookingQuote(String listingId, String roomId, String checkInDate, String checkOutDate,
                                int guests, int roomUnitsRequested, boolean selectedBreakfast, QuoteCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "bookings/quote");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("listingId", listingId);
                body.put("roomId", roomId);
                body.put("checkInDate", checkInDate);
                body.put("checkOutDate", checkOutDate);
                body.put("guests", guests);
                body.put("roomUnitsRequested", roomUnitsRequested);
                body.put("selectedBreakfast", selectedBreakfast);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    BookingQuote quote = BookingQuote.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(quote));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Quote Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void createBooking(String idToken, String listingId, String roomId, String checkInDate,
                              String checkOutDate, int guests, int roomUnitsRequested, boolean selectedBreakfast, BookingCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "bookings");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("listingId", listingId);
                body.put("roomId", roomId);
                body.put("checkInDate", checkInDate);
                body.put("checkOutDate", checkOutDate);
                body.put("guests", guests);
                body.put("roomUnitsRequested", roomUnitsRequested);
                body.put("selectedBreakfast", selectedBreakfast);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    Booking booking = Booking.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(booking));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Booking Request Failed (" + statusCode + ")")));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getTravelerBookings(String idToken, BookingsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "bookings/mine");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray data = res.optJSONArray("data");
                    List<Booking> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(Booking.fromJson(item));
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void cancelBooking(String idToken, String bookingId, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "bookings/" + bookingId + "/cancel");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Cancel Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}