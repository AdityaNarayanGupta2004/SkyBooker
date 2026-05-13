package com.skybooker.payment;

import com.skybooker.payment.dto.PaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.repository.PaymentRepository;
import com.skybooker.payment.service.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentServiceImpl;

    // Helper — ready-made Payment banao
    private Payment banaoPayment(String status) {
        Payment p = new Payment();
        p.setId(1L);
        p.setBookingId(101L);
        p.setUserEmail("rahul@gmail.com");
        p.setAmount(5000.0);
        p.setCurrency("INR");
        p.setPaymentMode("UPI");
        p.setStatus(status);
        p.setTransactionId("UPI-ABCD1234");
        p.setRefundAmount(0.0);
        p.setCreatedAt(LocalDateTime.now());
        p.setPaidAt(status.equals("PAID") ? LocalDateTime.now() : null);
        return p;
    }

    // Helper — ready-made PaymentRequest banao
    private PaymentRequest banaoRequest(String mode) {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(101L);
        req.setUserEmail("rahul@gmail.com");
        req.setAmount(5000.0);
        req.setPaymentMode(mode);
        return req;
    }

    // ---------------------------------------------------------------
    // INITIATE PAYMENT TESTS
    // ---------------------------------------------------------------

    // Test 1: UPI payment successfully ho jaye
    @Test
    void initiatePayment_WithUPI_ShouldSucceed() {
        PaymentRequest req = banaoRequest("UPI");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.initiatePayment(req);

        assertNotNull(res);
        assertEquals("PAID", res.getStatus());
        assertEquals("INR", res.getCurrency());
        assertEquals(5000.0, res.getAmount());
    }

    // Test 2: CARD payment successfully ho jaye
    @Test
    void initiatePayment_WithCard_ShouldSucceed() {
        PaymentRequest req = banaoRequest("CARD");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.initiatePayment(req);

        assertEquals("PAID", res.getStatus());
        // CARD ka transaction ID "CRD" se start hona chahiye
        assertTrue(res.getTransactionId().startsWith("CRD"));
    }

    // Test 3: NETBANKING payment ka transaction ID sahi prefix se shuru ho
    @Test
    void initiatePayment_WithNetBanking_ShouldHaveCorrectTxnPrefix() {
        PaymentRequest req = banaoRequest("NETBANKING");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.initiatePayment(req);

        assertTrue(res.getTransactionId().startsWith("NET"));
    }

    // Test 4: WALLET payment ka transaction ID sahi prefix se shuru ho
    @Test
    void initiatePayment_WithWallet_ShouldHaveCorrectTxnPrefix() {
        PaymentRequest req = banaoRequest("WALLET");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.initiatePayment(req);

        assertTrue(res.getTransactionId().startsWith("WLT"));
    }

    // Test 5: Ek booking ke liye dobara payment karne pe exception aaye
    @Test
    void initiatePayment_WhenAlreadyPaid_ShouldThrowException() {
        PaymentRequest req = banaoRequest("UPI");
        Payment existing = banaoPayment("PAID");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentServiceImpl.initiatePayment(req));

        assertTrue(ex.getMessage().contains("Payment already done"));
        // Save bilkul call nahi hona chahiye
        verify(paymentRepository, never()).save(any());
    }

    // Test 6: Payment ke baad paidAt set hona chahiye
    @Test
    void initiatePayment_ShouldSetPaidAt() {
        PaymentRequest req = banaoRequest("UPI");

        when(paymentRepository.findByBookingId(req.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.initiatePayment(req);

        assertNotNull(res.getPaidAt());
    }

    // ---------------------------------------------------------------
    // GET PAYMENT BY BOOKING TESTS
    // ---------------------------------------------------------------

    // Test 7: Booking ID se payment mile
    @Test
    void getPaymentByBooking_WhenExists_ShouldReturnPayment() {
        Payment payment = banaoPayment("PAID");

        when(paymentRepository.findByBookingId(101L))
                .thenReturn(Optional.of(payment));

        PaymentResponse res = paymentServiceImpl.getPaymentByBooking(101L);

        assertNotNull(res);
        assertEquals(101L, res.getBookingId());
        assertEquals("PAID", res.getStatus());
    }

    // Test 8: Booking ID na ho toh exception aaye
    @Test
    void getPaymentByBooking_WhenNotFound_ShouldThrowException() {
        when(paymentRepository.findByBookingId(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentServiceImpl.getPaymentByBooking(999L));

        assertTrue(ex.getMessage().contains("No payment found"));
    }

    // ---------------------------------------------------------------
    // GET PAYMENTS BY USER TESTS
    // ---------------------------------------------------------------

    // Test 9: User ke saare payments milein
    @Test
    void getPaymentsByUser_ShouldReturnAllUserPayments() {
        Payment p1 = banaoPayment("PAID");
        Payment p2 = banaoPayment("REFUNDED");
        p2.setId(2L);
        p2.setBookingId(102L);

        when(paymentRepository.findByUserEmail("rahul@gmail.com"))
                .thenReturn(List.of(p1, p2));

        List<PaymentResponse> result = paymentServiceImpl.getPaymentsByUser("rahul@gmail.com");

        assertEquals(2, result.size());
    }

    // Test 10: User ke koi payments nahi hain toh empty list aaye
    @Test
    void getPaymentsByUser_WhenNoPayments_ShouldReturnEmptyList() {
        when(paymentRepository.findByUserEmail("newuser@gmail.com"))
                .thenReturn(List.of());

        List<PaymentResponse> result = paymentServiceImpl.getPaymentsByUser("newuser@gmail.com");

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // REFUND TESTS
    // ---------------------------------------------------------------

    // Test 11: PAID payment ka refund ho jaye
    @Test
    void processRefund_WhenPaymentIsPaid_ShouldSucceed() {
        Payment payment = banaoPayment("PAID");

        when(paymentRepository.findByBookingId(101L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentServiceImpl.processRefund(101L);

        assertEquals("REFUNDED", res.getStatus());
        assertEquals(5000.0, res.getRefundAmount());
        assertNotNull(res.getRefundedAt());
    }

    // Test 12: PENDING payment ka refund nahi ho sakta
    @Test
    void processRefund_WhenPaymentIsPending_ShouldThrowException() {
        Payment payment = banaoPayment("PENDING");

        when(paymentRepository.findByBookingId(101L))
                .thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentServiceImpl.processRefund(101L));

        assertTrue(ex.getMessage().contains("Refund not possible"));
    }

    // Test 13: Pehle se REFUNDED payment ka dobara refund nahi ho sakta
    @Test
    void processRefund_WhenAlreadyRefunded_ShouldThrowException() {
        Payment payment = banaoPayment("REFUNDED");

        when(paymentRepository.findByBookingId(101L))
                .thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentServiceImpl.processRefund(101L));

        assertTrue(ex.getMessage().contains("Refund not possible"));
    }

    // Test 14: Payment na mile toh refund pe exception aaye
    @Test
    void processRefund_WhenPaymentNotFound_ShouldThrowException() {
        when(paymentRepository.findByBookingId(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentServiceImpl.processRefund(999L));

        assertTrue(ex.getMessage().contains("No payment found"));
    }

    // ---------------------------------------------------------------
    // GET BY STATUS TEST
    // ---------------------------------------------------------------

    // Test 15: Status se payments filter ho jayein
    @Test
    void getPaymentsByStatus_ShouldReturnMatchingPayments() {
        Payment p1 = banaoPayment("PAID");
        Payment p2 = banaoPayment("PAID");
        p2.setId(2L);

        when(paymentRepository.findByStatus("PAID"))
                .thenReturn(List.of(p1, p2));

        List<PaymentResponse> result = paymentServiceImpl.getPaymentsByStatus("PAID");

        assertEquals(2, result.size());
        result.forEach(r -> assertEquals("PAID", r.getStatus()));
    }
}
