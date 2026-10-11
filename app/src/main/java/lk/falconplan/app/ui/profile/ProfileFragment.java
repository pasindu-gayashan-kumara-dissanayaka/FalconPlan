package lk.falconplan.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.falconplan.app.R;

public class ProfileFragment extends Fragment {

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

        view.findViewById(R.id.btnSignIn).setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Design Preview: Authentication & Account Roles will be enabled in Phase 2.",
                        Toast.LENGTH_LONG).show()
        );

        RadioGroup radioGroupLanguage = view.findViewById(R.id.radioGroupLanguage);
        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLang = "English";
            if (checkedId == R.id.radioLangSinhala) {
                selectedLang = "Sinhala (සිංහල)";
            } else if (checkedId == R.id.radioLangTamil) {
                selectedLang = "Tamil (தமிழ்)";
            }
            Toast.makeText(requireContext(),
                    "Language set to: " + selectedLang,
                    Toast.LENGTH_SHORT).show();
        });
    }
}