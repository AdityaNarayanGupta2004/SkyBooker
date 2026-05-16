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
    private PaymentServiceImpl paymentService;

    private PaymentRequest createRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(101L);
        req.setUserEmail("test@gmail.com");
        req.setAmount(5000.0);
        req.setPaymentMode("UPI");
        return req;
    }

    @Test
    void initiatePayment_Success() {
        PaymentRequest req = createRequest();
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = paymentService.initiatePayment(req);
        assertEquals("PAID", res.getStatus());
        assertTrue(res.getTransactionId().startsWith("UPI-"));
    }

    @Test
    void initiatePayment_Duplicate_ThrowsException() {
        PaymentRequest req = createRequest();
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.of(new Payment()));
        assertThrows(IllegalStateException.class, () -> paymentService.initiatePayment(req));
    }

    @Test
    void getPaymentByBooking_Success() {
        Payment p = new Payment();
        p.setBookingId(101L);
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.of(p));
        assertEquals(101L, paymentService.getPaymentByBooking(101L).getBookingId());
    }

    @Test
    void processRefund_Success() {
        Payment p = new Payment();
        p.setStatus("PAID");
        p.setAmount(5000.0);
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.of(p));
        when(paymentRepository.save(any())).thenReturn(p);

        PaymentResponse res = paymentService.processRefund(101L);
        assertEquals("REFUNDED", res.getStatus());
    }

    @Test
    void processRefund_NotPaid_ThrowsException() {
        Payment p = new Payment();
        p.setStatus("PENDING");
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.of(p));
        assertThrows(IllegalStateException.class, () -> paymentService.processRefund(101L));
    }

    @Test
    void getPaymentsByUser_ReturnsList() {
        when(paymentRepository.findByUserEmail(anyString())).thenReturn(List.of(new Payment()));
        assertEquals(1, paymentService.getPaymentsByUser("a@b.com").size());
    }

    @Test
    void getPaymentsByStatus_ReturnsList() {
        when(paymentRepository.findByStatus("PAID")).thenReturn(List.of(new Payment()));
        assertEquals(1, paymentService.getPaymentsByStatus("PAID").size());
    }
}
