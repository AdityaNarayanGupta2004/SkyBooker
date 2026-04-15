package com.skybooker.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private String userEmail;

    private double amount;
    private String currency;  // INR

    // CARD, UPI, NETBANKING, WALLET
    private String paymentMode;

    // PENDING, PAID, FAILED, REFUNDED
    private String status;

    // gateway se jo transaction id milti hai
    private String transactionId;

    // refund ke time fill hoga
    private double refundAmount;
    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
