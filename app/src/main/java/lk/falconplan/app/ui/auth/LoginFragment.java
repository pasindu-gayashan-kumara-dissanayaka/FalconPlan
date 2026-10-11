package lk.falconplan.app.ui.auth;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executors;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.common.Resource;

public class LoginFragment extends Fragment {

    private AuthViewModel viewModel;
    private EditText editEmail;
    private EditText editPassword;
    private ProgressBar progressBar;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
            String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
            viewModel.loginWithEmail(email, password);
        });

        view.findViewById(R.id.btnForgotPassword).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new ForgotPasswordFragment(), true);
            }
        });

        view.findViewById(R.id.btnGoRegister).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new RegisterFragment(), true);
            }
        });

        view.findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> startGoogleSignIn());

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

    private void startGoogleSignIn() {
        CredentialManager credentialManager = CredentialManager.create(requireContext());

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(AuthEntryFragment.GOOGLE_WEB_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                            String idToken = googleIdTokenCredential.getIdToken();
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> viewModel.signInWithGoogleCredential(idToken));
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Google Sign-In canceled or unavailable: " + error.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                }
        );
    }
}