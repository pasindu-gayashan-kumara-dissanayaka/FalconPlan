package lk.falconplan.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Payment;
import lk.falconplan.app.data.repository.AdminPaymentRepository;
import lk.falconplan.app.data.repository.AdminPaymentRepositoryImpl;

public class AdminPaymentReviewFragment extends Fragment implements AdminPaymentAdapter.OnPaymentClickListener {

    private AdminPaymentRepository adminPaymentRepository;
    private AdminPaymentAdapter adapter;

    private RecyclerView recyclerPayments;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static AdminPaymentReviewFragment newInstance() {
        return new AdminPaymentReviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_payment_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminPaymentRepository = new AdminPaymentRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerPayments = view.findViewById(R.id.recyclerPayments);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminPaymentAdapter();
        recyclerPayments.setAdapter(adapter);
        adapter.setOnPaymentClickListener(this);

        loadAdminPayments();
    }

    private void loadAdminPayments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminPaymentRepository.getAdminPayments(idToken, "under_review", 1, 20, new AdminPaymentRepository.PaymentsListCallback() {
                @Override
                public void onSuccess(List<Payment> payments) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(payments);

                    if (payments == null || payments.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerPayments.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerPayments.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load payment review queue: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onItemClick(Payment payment) {
        if (getActivity() instanceof MainActivity && payment != null && payment.getPaymentId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(AdminPaymentDetailFragment.newInstance(payment.getPaymentId()), true);
        }
    }
}