package lk.falconplan.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.ui.auth.AuthEntryFragment;
import lk.falconplan.app.ui.auth.AuthViewModel;
import lk.falconplan.app.ui.auth.EmailVerificationFragment;
import lk.falconplan.app.ui.community.CommunityFragment;
import lk.falconplan.app.ui.explore.ExploreFragment;
import lk.falconplan.app.ui.language.LanguageSelectionFragment;
import lk.falconplan.app.ui.plans.PlansFragment;
import lk.falconplan.app.ui.plus.CreateBottomSheetFragment;
import lk.falconplan.app.ui.profile.ProfileFragment;
import lk.falconplan.app.ui.splash.SplashFragment;
import lk.falconplan.app.ui.splash.WelcomeFragment;
import lk.falconplan.app.utils.AppPreferences;
import lk.falconplan.app.utils.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private View fragmentContainer;
    private AuthViewModel authViewModel;
    private int currentSelectedTabId = -1;

    @Override
    protected void attachBaseContext(Context newBase) {
        AppPreferences prefs = AppPreferences.getInstance(newBase);
        String lang = prefs.getSelectedLanguage();
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation();
        observeAuthState();

        if (savedInstanceState == null) {
            // Display Splash Screen first on application startup
            loadFragment(new SplashFragment(), false);
            bottomNavigation.setVisibility(View.GONE);
        }
    }

    private void observeAuthState() {
        authViewModel.getAuthStateLiveData().observe(this, resource -> {
            if (resource == null) return;
            if (resource.getData() != null) {
                FirebaseUser user = resource.getData();
                if (user.isEmailVerified() || isGoogleUser(user)) {
                    navigateToMainContent();
                } else {
                    loadAuthFragment(new EmailVerificationFragment(), false);
                }
            } else if (resource.getStatus() == lk.falconplan.app.data.common.Resource.Status.ERROR) {
                checkSessionAndRoute();
            }
        });
    }

    private boolean isGoogleUser(FirebaseUser user) {
        if (user == null || user.getProviderData() == null) return false;
        for (var info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public void checkSessionAndRoute() {
        FirebaseUser currentUser = authViewModel.getCurrentUser();
        AppPreferences prefs = AppPreferences.getInstance(this);

        if (currentUser != null) {
            if (currentUser.isEmailVerified() || isGoogleUser(currentUser)) {
                navigateToMainContent();
            } else {
                loadAuthFragment(new EmailVerificationFragment(), false);
            }
        } else {
            if (!prefs.isOnboardingCompleted()) {
                showWelcomeScreen();
            } else if (!prefs.isLanguageSelected()) {
                showLanguageSelectionScreen();
            } else {
                showAuthEntryScreen();
            }
        }
    }

    public void showWelcomeScreen() {
        bottomNavigation.setVisibility(View.GONE);
        setContainerBottomMargin(0);
        currentSelectedTabId = -1;
        loadFragment(new WelcomeFragment(), false);
    }

    public void showLanguageSelectionScreen() {
        bottomNavigation.setVisibility(View.GONE);
        setContainerBottomMargin(0);
        currentSelectedTabId = -1;
        loadFragment(new LanguageSelectionFragment(), false);
    }

    public void showAuthEntryScreen() {
        bottomNavigation.setVisibility(View.GONE);
        setContainerBottomMargin(0);
        currentSelectedTabId = -1;
        loadFragment(new AuthEntryFragment(), false);
    }

    public void loadAuthFragment(Fragment fragment, boolean addToBackStack) {
        bottomNavigation.setVisibility(View.GONE);
        setContainerBottomMargin(0);
        loadFragment(fragment, addToBackStack);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_plus) {
                // Center Plus (+) opens polished BottomSheet
                CreateBottomSheetFragment bottomSheet = CreateBottomSheetFragment.newInstance();
                bottomSheet.show(getSupportFragmentManager(), CreateBottomSheetFragment.TAG);
                return false;
            }

            Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (itemId == currentSelectedTabId && currentFrag != null && !(currentFrag instanceof WelcomeFragment) && !(currentFrag instanceof AuthEntryFragment)) {
                return true;
            }

            currentSelectedTabId = itemId;

            if (itemId == R.id.nav_community) {
                loadFragment(new CommunityFragment(), true);
                return true;
            } else if (itemId == R.id.nav_explore) {
                loadFragment(new ExploreFragment(), true);
                return true;
            } else if (itemId == R.id.nav_plans) {
                loadFragment(new PlansFragment(), true);
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), true);
                return true;
            }

            return false;
        });
    }

    public void navigateToMainContent() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        bottomNavigation.setVisibility(View.VISIBLE);
        setContainerBottomMargin((int) (64 * getResources().getDisplayMetrics().density));
        currentSelectedTabId = R.id.nav_community;
        bottomNavigation.setSelectedItemId(R.id.nav_community);
        loadFragment(new CommunityFragment(), false);
    }

    public void switchToTab(@IdRes int tabId) {
        if (bottomNavigation.getVisibility() != View.VISIBLE) {
            bottomNavigation.setVisibility(View.VISIBLE);
            setContainerBottomMargin((int) (64 * getResources().getDisplayMetrics().density));
        }
        bottomNavigation.setSelectedItemId(tabId);
    }

    private void setContainerBottomMargin(int marginPx) {
        if (fragmentContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            params.bottomMargin = marginPx;
            fragmentContainer.setLayoutParams(params);
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}