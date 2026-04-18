package com.skybooker.payment.service;

import com.skybooker.payment.dto.PaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse initiatePayment(PaymentRequest request);

    PaymentResponse getPaymentByBooking(Long bookingId);

    List<PaymentResponse> getPaymentsByUser(String userEmail);

    PaymentResponse processRefund(Long bookingId);

    List<PaymentResponse> getPaymentsByStatus(String status);
}
