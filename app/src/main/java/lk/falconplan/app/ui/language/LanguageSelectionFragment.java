package lk.falconplan.app.ui.language;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.utils.AppPreferences;
import lk.falconplan.app.utils.LocaleHelper;

public class LanguageSelectionFragment extends Fragment {

    public static LanguageSelectionFragment newInstance() {
        return new LanguageSelectionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioGroup radioGroupLanguage = view.findViewById(R.id.radioGroupLanguage);

        view.findViewById(R.id.btnContinueLanguage).setOnClickListener(v -> {
            int checkedId = radioGroupLanguage.getCheckedRadioButtonId();
            String langCode = "en";
            if (checkedId == R.id.radioLangSi) {
                langCode = "si";
            } else if (checkedId == R.id.radioLangTa) {
                langCode = "ta";
            }

            AppPreferences prefs = AppPreferences.getInstance(requireContext());
            prefs.setSelectedLanguage(langCode);
            prefs.setLanguageSelected(true);

            LocaleHelper.applyLocale(requireContext(), langCode);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).checkSessionAndRoute();
            }
        });
    }
}