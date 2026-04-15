package com.skybooker.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    private Long bookingId;
    private String userEmail;
    private double amount;
    private String paymentMode;  // CARD, UPI, NETBANKING, WALLET
}
