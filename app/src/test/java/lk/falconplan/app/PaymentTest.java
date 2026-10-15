package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.Payment;

import static org.junit.Assert.*;

public class PaymentTest {

    @Test
    public void payment_awaitingProofFormatting_returnsAwaitingProof() {
        Payment payment = new Payment(
                "booking_101",
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                19000,
                "LKR",
                Payment.METHOD_BANK_TRANSFER,
                Payment.STATUS_AWAITING_PROOF,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Awaiting Bank Receipt Proof", payment.getFormattedStatus());
        assertEquals("Rs. 19,000 LKR", payment.getFormattedAmount());
    }

    @Test
    public void payment_underReviewFormatting_returnsProofSubmittedUnderReview() {
        Payment payment = new Payment(
                "booking_101",
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                19000,
                "LKR",
                Payment.METHOD_BANK_TRANSFER,
                Payment.STATUS_UNDER_REVIEW,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Proof Submitted – Under Review", payment.getFormattedStatus());
    }

    @Test
    public void payment_dueOnArrivalFormatting_returnsDueOnArrivalCash() {
        Payment payment = new Payment(
                "booking_101",
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                19000,
                "LKR",
                Payment.METHOD_CASH_ON_ARRIVAL,
                Payment.STATUS_DUE_ON_ARRIVAL,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Due On Arrival (Cash)", payment.getFormattedStatus());
    }
}