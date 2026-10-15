package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.RoomPricing;
import lk.falconplan.app.data.repository.RoomPricingRepository;
import lk.falconplan.app.data.repository.RoomPricingRepositoryImpl;

public class RoomPricingFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_ROOM_ID = "arg_room_id";

    private RoomPricingRepository pricingRepository;
    private String listingId;
    private String roomId;

    private TextView txtRoomName;
    private TextView txtRoomOverview;

    private EditText editPricePerNight;
    private EditText editIncludedGuests;
    private EditText editExtraGuestFee;
    private MaterialSwitch switchBreakfastOffered;
    private View layoutBreakfastFee;
    private EditText editBreakfastFee;

    private ProgressBar progressBar;

    public static RoomPricingFragment newInstance(String listingId, String roomId) {
        RoomPricingFragment fragment = new RoomPricingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_pricing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pricingRepository = new RoomPricingRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";
        roomId = getArguments() != null ? getArguments().getString(ARG_ROOM_ID) : "";

        txtRoomName = view.findViewById(R.id.txtRoomName);
        txtRoomOverview = view.findViewById(R.id.txtRoomOverview);

        editPricePerNight = view.findViewById(R.id.editPricePerNight);
        editIncludedGuests = view.findViewById(R.id.editIncludedGuests);
        editExtraGuestFee = view.findViewById(R.id.editExtraGuestFee);
        switchBreakfastOffered = view.findViewById(R.id.switchBreakfastOffered);
        layoutBreakfastFee = view.findViewById(R.id.layoutBreakfastFee);
        editBreakfastFee = view.findViewById(R.id.editBreakfastFee);

        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        switchBreakfastOffered.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutBreakfastFee.setVisibility(View.VISIBLE);
            } else {
                layoutBreakfastFee.setVisibility(View.GONE);
            }
        });

        view.findViewById(R.id.btnSavePricing).setOnClickListener(v -> savePricing());

        loadSavedPricing();
    }

    private void loadSavedPricing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty() || roomId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        pricingRepository.getRoomPricing(user.getUid(), listingId, roomId, new RoomPricingRepository.PricingCallback() {
            @Override
            public void onSuccess(RoomPricing pricing) {
                progressBar.setVisibility(View.GONE);
                if (pricing != null) {
                    populatePricingFields(pricing);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void populatePricingFields(RoomPricing pricing) {
        editPricePerNight.setText(String.valueOf(pricing.getPricePerNightLkr()));
        editIncludedGuests.setText(String.valueOf(pricing.getIncludedGuests()));
        editExtraGuestFee.setText(String.valueOf(pricing.getExtraGuestFeeLkr()));
        switchBreakfastOffered.setChecked(pricing.isBreakfastOffered());
        if (pricing.isBreakfastOffered()) {
            layoutBreakfastFee.setVisibility(View.VISIBLE);
            editBreakfastFee.setText(String.valueOf(pricing.getBreakfastFeePerGuestLkr()));
        } else {
            layoutBreakfastFee.setVisibility(View.GONE);
        }
    }

    private void savePricing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty() || roomId.isEmpty()) {
            Toast.makeText(requireContext(), "Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        long pricePerNight = parseLongSafe(editPricePerNight.getText() != null ? editPricePerNight.getText().toString() : "0");
        int includedGuests = parseIntSafe(editIncludedGuests.getText() != null ? editIncludedGuests.getText().toString() : "1");
        long extraGuestFee = parseLongSafe(editExtraGuestFee.getText() != null ? editExtraGuestFee.getText().toString() : "0");
        boolean breakfastOffered = switchBreakfastOffered.isChecked();
        long breakfastFee = breakfastOffered ? parseLongSafe(editBreakfastFee.getText() != null ? editBreakfastFee.getText().toString() : "0") : 0;

        if (pricePerNight <= 0) {
            Toast.makeText(requireContext(), "Base Price per Night must be greater than 0 LKR.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (includedGuests <= 0) {
            Toast.makeText(requireContext(), "Included Guests must be at least 1.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        long now = System.currentTimeMillis();

        RoomPricing pricing = new RoomPricing(
                RoomPricing.CURRENCY_LKR,
                pricePerNight,
                includedGuests,
                extraGuestFee,
                breakfastOffered,
                breakfastFee,
                now,
                now
        );

        pricingRepository.savePricing(user.getUid(), listingId, roomId, pricing, new RoomPricingRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Room Pricing Saved Successfully!", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to save pricing: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long parseLongSafe(String val) {
        try {
            return Long.parseLong(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 1;
        }
    }
}