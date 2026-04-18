package com.skybooker.payment.repository;

import com.skybooker.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByUserEmail(String userEmail);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByTransactionId(String transactionId);
}
