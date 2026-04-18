package com.skybooker.flight.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String flightNumber;
    @Column(nullable = false)
    private String airline;
    @Column(nullable = false)
    private String source;
    @Column(nullable = false)
    private String destination;

    private LocalDate departureDate;
    private String departureTime;

    // arrivalDate alag column — overnight flights ke liye
    private LocalDate arrivalDate;
    private String arrivalTime;

    private int totalSeats;
    private int availableSeats;
    private double price;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}