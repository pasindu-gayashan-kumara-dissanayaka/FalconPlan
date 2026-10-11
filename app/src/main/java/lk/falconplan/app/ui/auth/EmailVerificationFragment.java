package lk.falconplan.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.common.Resource;

public class EmailVerificationFragment extends Fragment {

    private AuthViewModel viewModel;
    private TextView txtEmailInfo;
    private ProgressBar progressBar;

    public static EmailVerificationFragment newInstance() {
        return new EmailVerificationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        txtEmailInfo = view.findViewById(R.id.txtEmailInfo);
        progressBar = view.findViewById(R.id.progressBar);

        FirebaseUser user = viewModel.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            txtEmailInfo.setText("We sent a verification link to " + user.getEmail() + ". Please check your inbox and click the link to activate your FalconPlan account.");
        }

        view.findViewById(R.id.btnCheckVerified).setOnClickListener(v -> viewModel.checkEmailVerification());

        view.findViewById(R.id.btnResendEmail).setOnClickListener(v -> viewModel.sendEmailVerification());

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            viewModel.signOut();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new AuthEntryFragment(), false);
            }
        });

        viewModel.getOperationStatusLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}