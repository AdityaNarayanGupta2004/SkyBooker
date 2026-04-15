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

    private String flightNumber;

    private String airline;

    private String source;

    private String destination;

    private LocalDate departureDate;

    private String departureTime;

    private String arrivalTime;

    private int totalSeats;

    private int availableSeats;

    private double price;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}