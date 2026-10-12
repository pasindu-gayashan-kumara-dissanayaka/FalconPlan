package lk.falconplan.app.ui.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.ui.plans.CreatePlanFragment;

public class CreateBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "CreateBottomSheetFragment";

    public static CreateBottomSheetFragment newInstance() {
        return new CreateBottomSheetFragment();
    }

    @Override
    public int getTheme() {
        return R.style.Theme_FalconPlan_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.cardOptionStory).setOnClickListener(v -> showPlaceholderToast(getString(R.string.plus_sheet_story)));

        view.findViewById(R.id.cardOptionTravelPlan).setOnClickListener(v -> {
            dismiss();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(new CreatePlanFragment(), true);
            }
        });

        view.findViewById(R.id.cardOptionPost).setOnClickListener(v -> showPlaceholderToast(getString(R.string.plus_sheet_post)));
    }

    private void showPlaceholderToast(String featureName) {
        Toast.makeText(requireContext(),
                "Design Preview: " + featureName + " creation will be enabled in upcoming phase.",
                Toast.LENGTH_LONG).show();
        dismiss();
    }
}