package lk.falconplan.app.ui.reviews;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ReviewItem;
import lk.falconplan.app.data.repository.ReviewRepository;
import lk.falconplan.app.data.repository.ReviewRepositoryImpl;

public class WriteHomestayReviewFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";

    private ReviewRepository repository;

    private String bookingId;

    private RatingBar ratingBar;
    private EditText editReviewText;
    private MaterialButton btnSubmitReview;
    private ProgressBar progressBar;

    public static WriteHomestayReviewFragment newInstance(String bookingId) {
        WriteHomestayReviewFragment fragment = new WriteHomestayReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write_homestay_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ReviewRepositoryImpl();

        bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID, "") : "";

        ratingBar = view.findViewById(R.id.ratingBar);
        editReviewText = view.findViewById(R.id.editReviewText);
        btnSubmitReview = view.findViewById(R.id.btnSubmitReview);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        int rating = (int) ratingBar.getRating();
        String reviewText = editReviewText.getText() != null ? editReviewText.getText().toString().trim() : "";

        if (rating < 1 || rating > 5) {
            Toast.makeText(requireContext(), "Please select a star rating between 1 and 5.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            repository.submitReview(idToken, bookingId, rating, reviewText, new ReviewRepository.ReviewSubmitCallback() {
                @Override
                public void onSuccess(ReviewItem review) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(requireContext(), "Thank you! Your verified review has been published.", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to submit review: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}