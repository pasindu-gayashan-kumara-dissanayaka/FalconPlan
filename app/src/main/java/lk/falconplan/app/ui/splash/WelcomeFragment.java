package lk.falconplan.app.ui.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;

public class WelcomeFragment extends Fragment {

    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View.OnClickListener startListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToMainContent();
            }
        };

        view.findViewById(R.id.btnGetStarted).setOnClickListener(startListener);
        view.findViewById(R.id.btnSkip).setOnClickListener(startListener);
    }
}