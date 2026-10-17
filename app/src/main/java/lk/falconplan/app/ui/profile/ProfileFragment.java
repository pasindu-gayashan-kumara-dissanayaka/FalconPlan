package lk.falconplan.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.common.Resource;
import lk.falconplan.app.data.model.ProviderProfile;
import lk.falconplan.app.data.model.UserProfile;
import lk.falconplan.app.data.repository.ProviderProfileRepository;
import lk.falconplan.app.data.repository.ProviderProfileRepositoryImpl;
import lk.falconplan.app.ui.auth.AuthEntryFragment;
import lk.falconplan.app.ui.auth.AuthViewModel;
import lk.falconplan.app.ui.provider.ProviderRegistrationFragment;
import lk.falconplan.app.ui.provider.ProviderSetupStatusFragment;
import lk.falconplan.app.utils.AppPreferences;
import lk.falconplan.app.utils.LocaleHelper;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;
    private ProviderProfileRepository providerProfileRepository;

    private View cardSignedInProfile;
    private View cardGuestProfile;
    private View cardProviderNotice;
    private TextView txtUserName;
    private TextView txtUserEmail;
    private TextView txtVerificationBadge;
    private MaterialButtonToggleGroup toggleGroupMode;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        providerProfileRepository = new ProviderProfileRepositoryImpl();

        cardSignedInProfile = view.findViewById(R.id.cardSignedInProfile);
        cardGuestProfile = view.findViewById(R.id.cardGuestProfile);
        cardProviderNotice = view.findViewById(R.id.cardProviderNotice);
        txtUserName = view.findViewById(R.id.txtUserName);
        txtUserEmail = view.findViewById(R.id.txtUserEmail);
        txtVerificationBadge = view.findViewById(R.id.txtVerificationBadge);
        toggleGroupMode = view.findViewById(R.id.toggleGroupMode);

        view.findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new AuthEntryFragment(), true);
            }
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(new EditProfileFragment(), true);
            }
        });

        View btnMyBookings = view.findViewById(R.id.btnMyBookings);
        if (btnMyBookings != null) {
            btnMyBookings.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.booking.MyBookingsFragment.newInstance(), true);
                }
            });
        }

        View btnMyPayments = view.findViewById(R.id.btnMyPayments);
        if (btnMyPayments != null) {
            btnMyPayments.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.booking.MyPaymentsFragment.newInstance(), true);
                }
            });
        }

        View btnAdminPaymentReviews = view.findViewById(R.id.btnAdminPaymentReviews);
        if (btnAdminPaymentReviews != null) {
            btnAdminPaymentReviews.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.admin.AdminPaymentReviewFragment.newInstance(), true);
                }
            });
        }

        View btnMySavedPosts = view.findViewById(R.id.btnMySavedPosts);
        if (btnMySavedPosts != null) {
            btnMySavedPosts.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.community.MySavedCommunityPostsFragment.newInstance(), true);
                }
            });
        }

        View btnMyBookingChats = view.findViewById(R.id.btnMyBookingChats);
        if (btnMyBookingChats != null) {
            btnMyBookingChats.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.chat.MyChatConversationsFragment.newInstance(), true);
                }
            });
        }

        View btnMyNotifications = view.findViewById(R.id.btnMyNotifications);
        if (btnMyNotifications != null) {
            btnMyNotifications.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(lk.falconplan.app.ui.notifications.MyNotificationsFragment.newInstance(), true);
                }
            });
        }

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            authViewModel.signOut();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showAuthEntryScreen();
            }
        });

        cardProviderNotice.setOnClickListener(v -> openProviderWorkspace());

        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnModeProvider) {
                authViewModel.updateActiveMode(UserProfile.MODE_PROVIDER);
                cardProviderNotice.setVisibility(View.VISIBLE);
                openProviderWorkspace();
            } else {
                authViewModel.updateActiveMode(UserProfile.MODE_TRAVELER);
                cardProviderNotice.setVisibility(View.GONE);
            }
        });

        // Initialize Appearance Selection
        AppPreferences prefs = AppPreferences.getInstance(requireContext());
        RadioGroup radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        int currentTheme = prefs.getThemeMode();
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            radioGroupTheme.check(R.id.radioThemeDark);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            radioGroupTheme.check(R.id.radioThemeLight);
        } else {
            radioGroupTheme.check(R.id.radioThemeSystem);
        }

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.radioThemeLight) {
                selectedThemeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioThemeDark) {
                selectedThemeMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            prefs.setThemeMode(selectedThemeMode);
            AppCompatDelegate.setDefaultNightMode(selectedThemeMode);
        });

        // Initialize Language Selection
        RadioGroup radioGroupLanguage = view.findViewById(R.id.radioGroupLanguage);
        String currentLang = prefs.getSelectedLanguage();
        if ("si".equalsIgnoreCase(currentLang)) {
            radioGroupLanguage.check(R.id.radioLangSinhala);
        } else if ("ta".equalsIgnoreCase(currentLang)) {
            radioGroupLanguage.check(R.id.radioLangTamil);
        } else {
            radioGroupLanguage.check(R.id.radioLangEnglish);
        }

        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLang = "en";
            String displayLang = "English";
            if (checkedId == R.id.radioLangSinhala) {
                selectedLang = "si";
                displayLang = "Sinhala (සිංහල)";
            } else if (checkedId == R.id.radioLangTamil) {
                selectedLang = "ta";
                displayLang = "Tamil (தமிழ்)";
            }

            if (!selectedLang.equalsIgnoreCase(prefs.getSelectedLanguage())) {
                prefs.setSelectedLanguage(selectedLang);
                LocaleHelper.applyLocale(requireContext(), selectedLang);
                Toast.makeText(requireContext(),
                        getString(R.string.lang_switched_msg, displayLang),
                        Toast.LENGTH_SHORT).show();
                requireActivity().recreate();
            }
        });

        updateUiState();

        authViewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                UserProfile profile = resource.getData();
                txtUserName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "FalconPlan Member");
                if (UserProfile.MODE_PROVIDER.equals(profile.getActiveMode())) {
                    toggleGroupMode.check(R.id.btnModeProvider);
                    cardProviderNotice.setVisibility(View.VISIBLE);
                } else {
                    toggleGroupMode.check(R.id.btnModeTraveler);
                    cardProviderNotice.setVisibility(View.GONE);
                }
            }
        });
    }

    private void openProviderWorkspace() {
        FirebaseUser user = authViewModel.getCurrentUser();
        if (user == null) return;

        providerProfileRepository.getProviderProfile(user.getUid(), new ProviderProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProviderProfile profile) {
                if (getActivity() instanceof MainActivity) {
                    if (profile != null) {
                        ((MainActivity) getActivity()).loadMainContentFragment(ProviderSetupStatusFragment.newInstance(), true);
                    } else {
                        ((MainActivity) getActivity()).loadMainContentFragment(ProviderRegistrationFragment.newInstance(), true);
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(ProviderRegistrationFragment.newInstance(), true);
                }
            }
        });
    }

    private void updateUiState() {
        FirebaseUser user = authViewModel.getCurrentUser();
        View btnSignOut = requireView().findViewById(R.id.btnSignOut);

        if (user != null) {
            cardSignedInProfile.setVisibility(View.VISIBLE);
            cardGuestProfile.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);

            txtUserName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "FalconPlan User");
            txtUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            if (user.isEmailVerified()) {
                txtVerificationBadge.setText("Email Verified");
            } else {
                txtVerificationBadge.setText("Unverified Email");
            }

            authViewModel.loadProfile(user.getUid());
        } else {
            cardSignedInProfile.setVisibility(View.GONE);
            cardGuestProfile.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
        }
    }
}