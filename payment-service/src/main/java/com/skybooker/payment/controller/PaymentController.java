package com.skybooker.payment.controller;

import com.skybooker.payment.dto.PaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // payment karo - booking ke baad call hoga
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    // booking ka payment status dekho
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getByBooking(@PathVariable Long bookingId) {
        PaymentResponse response = paymentService.getPaymentByBooking(bookingId);
        return ResponseEntity.ok(response);
    }

    // user ke saare payments dekho
    @GetMapping("/user/{email}")
    public ResponseEntity<List<PaymentResponse>> getByUser(@PathVariable String email) {
        List<PaymentResponse> responses = paymentService.getPaymentsByUser(email);
        return ResponseEntity.ok(responses);
    }

    // refund karo booking cancel hone par
    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long bookingId) {
        PaymentResponse response = paymentService.processRefund(bookingId);
        return ResponseEntity.ok(response);
    }

    // status se payments filter karo - admin ke liye
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getByStatus(@PathVariable String status) {
        List<PaymentResponse> responses = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(responses);
    }
}
