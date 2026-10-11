package lk.falconplan.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class AuthEntryFragment extends Fragment {

    public static final String GOOGLE_WEB_CLIENT_ID = "333753747421-3u096eqtbqd8me55dd7inq36bqpll14v.apps.googleusercontent.com";

    private AuthViewModel viewModel;

    public static AuthEntryFragment newInstance() {
        return new AuthEntryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        view.findViewById(R.id.btnLoginEmail).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new LoginFragment(), true);
            }
        });

        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new RegisterFragment(), true);
            }
        });

        view.findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> startGoogleSignIn());

        view.findViewById(R.id.btnContinueGuest).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToMainContent();
            }
        });
    }

    private void startGoogleSignIn() {
        CredentialManager credentialManager = CredentialManager.create(requireContext());

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
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