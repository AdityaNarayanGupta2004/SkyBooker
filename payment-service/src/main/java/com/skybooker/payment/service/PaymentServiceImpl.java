//package com.skybooker.payment.service;
//
//import com.skybooker.payment.dto.PaymentRequest;
//import com.skybooker.payment.dto.PaymentResponse;
//import com.skybooker.payment.entity.Payment;
//import com.skybooker.payment.repository.PaymentRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentServiceImpl implements PaymentService {
//
//    private final PaymentRepository paymentRepository;
//
//    @Override
//    public PaymentResponse initiatePayment(PaymentRequest request) {
//
//        // ek booking ka payment pehle se exist toh nahi karta check karo
//        boolean alreadyPaid = paymentRepository.findByBookingId(request.getBookingId()).isPresent();
//        if (alreadyPaid) {
//            throw new RuntimeException("Payment already done for booking: " + request.getBookingId());
//        }
//
//        // payment record banao - status PENDING rakho pehle
//        Payment payment = new Payment();
//        payment.setBookingId(request.getBookingId());
//        payment.setUserEmail(request.getUserEmail());
//        payment.setAmount(request.getAmount());
//        payment.setCurrency("INR");
//        payment.setPaymentMode(request.getPaymentMode());
//        payment.setStatus("PENDING");
//        payment.setCreatedAt(LocalDateTime.now());
//
//        // real Razorpay nahi hai abhi - transaction id generate karo simulate karne ke liye
//        // production mein yahan Razorpay API call hogi
//        String transactionId = generateTransactionId(request.getPaymentMode());
//        payment.setTransactionId(transactionId);
//
//        // payment simulate - real mein gateway confirm karega
//        // abhi directly PAID mark kar rahe hain
//        payment.setStatus("PAID");
//        payment.setPaidAt(LocalDateTime.now());
//
//        Payment saved = paymentRepository.save(payment);
//
//        return mapToResponse(saved, "Payment successful");
//    }
//
//    @Override
//    public PaymentResponse getPaymentByBooking(Long bookingId) {
//        Payment payment = paymentRepository.findByBookingId(bookingId)
//                .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId));
//        return mapToResponse(payment, "Success");
//    }
//
//    @Override
//    public List<PaymentResponse> getPaymentsByUser(String userEmail) {
//        List<Payment> payments = paymentRepository.findByUserEmail(userEmail);
//        return payments.stream()
//                .map(p -> mapToResponse(p, "Success"))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public PaymentResponse processRefund(Long bookingId) {
//        Payment payment = paymentRepository.findByBookingId(bookingId)
//                .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId));
//
//        // sirf PAID payments ka refund hoga
//        if (!payment.getStatus().equals("PAID")) {
//            throw new RuntimeException("Refund not possible. Payment status is: " + payment.getStatus());
//        }
//
//        // pura amount refund karo
//        payment.setStatus("REFUNDED");
//        payment.setRefundAmount(payment.getAmount());
//        payment.setRefundedAt(LocalDateTime.now());
//
//        Payment updated = paymentRepository.save(payment);
//        return mapToResponse(updated, "Refund processed successfully. Amount will be credited in 5-7 working days.");
//    }
//
//    @Override
//    public List<PaymentResponse> getPaymentsByStatus(String status) {
//        List<Payment> payments = paymentRepository.findByStatus(status);
//        return payments.stream()
//                .map(p -> mapToResponse(p, "Success"))
//                .collect(Collectors.toList());
//    }
//
//    // transaction id generate karo - mode ke hisaab se prefix
//    private String generateTransactionId(String mode) {
//        String prefix;
//        switch (mode.toUpperCase()) {
//            case "UPI"        -> prefix = "UPI";
//            case "CARD"       -> prefix = "CRD";
//            case "NETBANKING" -> prefix = "NET";
//            case "WALLET"     -> prefix = "WLT";
//            default           -> prefix = "PAY";
//        }
//        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//    private PaymentResponse mapToResponse(Payment payment, String message) {
//        PaymentResponse res = new PaymentResponse();
//        res.setPaymentId(payment.getId());
//        res.setBookingId(payment.getBookingId());
//        res.setUserEmail(payment.getUserEmail());
//        res.setAmount(payment.getAmount());
//        res.setCurrency(payment.getCurrency());
//        res.setPaymentMode(payment.getPaymentMode());
//        res.setStatus(payment.getStatus());
//        res.setTransactionId(payment.getTransactionId());
//        res.setRefundAmount(payment.getRefundAmount());
//        res.setPaidAt(payment.getPaidAt());
//        res.setRefundedAt(payment.getRefundedAt());
//        res.setMessage(message);
//        return res;
//    }
//}

package com.skybooker.payment.service;

import com.skybooker.payment.dto.PaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Payment request — bookingId: {}, user: {}, amount: {}, mode: {}",
                request.getBookingId(), request.getUserEmail(),
                request.getAmount(), request.getPaymentMode());

        boolean alreadyPaid = paymentRepository.findByBookingId(request.getBookingId()).isPresent();
        if (alreadyPaid) {
            log.warn("Duplicate payment attempt — bookingId: {}", request.getBookingId());
            throw new RuntimeException("Payment already done for booking: " + request.getBookingId());
        }

        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(request.getUserEmail());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setPaymentMode(request.getPaymentMode());
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());

        String transactionId = generateTransactionId(request.getPaymentMode());
        payment.setTransactionId(transactionId);
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment successful — bookingId: {}, txnId: {}, amount: ₹{}",
                saved.getBookingId(), saved.getTransactionId(), saved.getAmount());
        return mapToResponse(saved, "Payment successful");
    }

    @Override
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        log.debug("Fetching payment — bookingId: {}", bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> {
                    log.warn("Payment not found — bookingId: {}", bookingId);
                    return new RuntimeException("No payment found for booking: " + bookingId);
                });
        return mapToResponse(payment, "Success");
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(String userEmail) {
        log.debug("Fetching payments for user: {}", userEmail);
        return paymentRepository.findByUserEmail(userEmail).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    @Override
    public PaymentResponse processRefund(Long bookingId) {
        log.info("Refund request — bookingId: {}", bookingId);

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> {
                    log.warn("Refund failed — payment not found: bookingId: {}", bookingId);
                    return new RuntimeException("No payment found for booking: " + bookingId);
                });

        if (!payment.getStatus().equals("PAID")) {
            log.warn("Refund rejected — status is not PAID. bookingId: {}, status: {}",
                    bookingId, payment.getStatus());
            throw new RuntimeException("Refund not possible. Payment status is: " + payment.getStatus());
        }

        payment.setStatus("REFUNDED");
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundedAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);
        log.info("Refund processed — bookingId: {}, amount: ₹{}", bookingId, payment.getAmount());
        return mapToResponse(updated, "Refund processed successfully. Amount will be credited in 5-7 working days.");
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        log.debug("Fetching payments by status: {}", status);
        return paymentRepository.findByStatus(status).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    private String generateTransactionId(String mode) {
        String prefix = switch (mode.toUpperCase()) {
            case "UPI"        -> "UPI";
            case "CARD"       -> "CRD";
            case "NETBANKING" -> "NET";
            case "WALLET"     -> "WLT";
            default           -> "PAY";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        PaymentResponse res = new PaymentResponse();
        res.setPaymentId(payment.getId());
        res.setBookingId(payment.getBookingId());
        res.setUserEmail(payment.getUserEmail());
        res.setAmount(payment.getAmount());
        res.setCurrency(payment.getCurrency());
        res.setPaymentMode(payment.getPaymentMode());
        res.setStatus(payment.getStatus());
        res.setTransactionId(payment.getTransactionId());
        res.setRefundAmount(payment.getRefundAmount());
        res.setPaidAt(payment.getPaidAt());
        res.setRefundedAt(payment.getRefundedAt());
        res.setMessage(message);
        return res;
    }
}
