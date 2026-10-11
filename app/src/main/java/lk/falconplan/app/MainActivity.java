package lk.falconplan.app;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import lk.falconplan.app.ui.community.CommunityFragment;
import lk.falconplan.app.ui.explore.ExploreFragment;
import lk.falconplan.app.ui.plans.PlansFragment;
import lk.falconplan.app.ui.plus.CreateBottomSheetFragment;
import lk.falconplan.app.ui.profile.ProfileFragment;
import lk.falconplan.app.ui.splash.WelcomeFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private int currentSelectedTabId = R.id.nav_explore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation();

        if (savedInstanceState == null) {
            // Load Welcome Fragment initially for first-launch experience
            loadFragment(new WelcomeFragment(), false);
            bottomNavigation.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_plus) {
                // Center Plus (+) opens polished BottomSheet
                CreateBottomSheetFragment bottomSheet = CreateBottomSheetFragment.newInstance();
                bottomSheet.show(getSupportFragmentManager(), CreateBottomSheetFragment.TAG);
                // Keep the current active tab selection intact
                return false;
            }

            if (itemId == currentSelectedTabId && getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
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
        bottomNavigation.setVisibility(View.VISIBLE);
        switchToTab(R.id.nav_explore);
    }

    public void switchToTab(@IdRes int tabId) {
        if (bottomNavigation.getVisibility() != View.VISIBLE) {
            bottomNavigation.setVisibility(View.VISIBLE);
        }
        bottomNavigation.setSelectedItemId(tabId);
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