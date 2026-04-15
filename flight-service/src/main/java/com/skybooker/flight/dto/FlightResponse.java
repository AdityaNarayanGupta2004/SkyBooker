package com.skybooker.flight.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightResponse {

    private Long id;
    private String flightNumber;
    private String airline;
    private String source;
    private String destination;
    private LocalDate departureDate;
    private String departureTime;
    private String arrivalTime;
    private int availableSeats;
    private double price;
}