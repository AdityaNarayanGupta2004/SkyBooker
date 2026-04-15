package com.skybooker.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flightId;
    private String userEmail;

    private int seatsBooked;
    private double totalPrice;

    private String status;

    private LocalDateTime bookingTime;
}