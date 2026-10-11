package lk.falconplan.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;

import lk.falconplan.app.data.common.Resource;
import lk.falconplan.app.data.model.UserProfile;

public class RegisterFragment extends Fragment {

    private AuthViewModel viewModel;
    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private RadioGroup radioGroupRole;
    private ProgressBar progressBar;

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        editName = view.findViewById(R.id.editName);
        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        radioGroupRole = view.findViewById(R.id.radioGroupRole);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
            String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
            String confirmPassword = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString().trim() : "";

            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            String activeMode = UserProfile.MODE_TRAVELER;
            if (radioGroupRole.getCheckedRadioButtonId() == R.id.radioRoleProvider) {
                activeMode = UserProfile.MODE_PROVIDER;
            }

            viewModel.registerWithEmail(name, email, password, activeMode);
        });

        view.findViewById(R.id.btnGoLogin).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new LoginFragment(), true);
            }
        });

        viewModel.getAuthStateLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                if (resource.getStatus() == Resource.Status.ERROR) {
                    Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        viewModel.getOperationStatusLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}