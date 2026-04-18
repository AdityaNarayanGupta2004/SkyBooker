package com.skybooker.flight.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightRequest {

    private String flightNumber;
    private String airline;
    private String source;
    private String destination;

    private LocalDate departureDate;
    private String departureTime;

    // arrivalDate alag — overnight flight possible
    private LocalDate arrivalDate;
    private String arrivalTime;

    private int totalSeats;
    private double price;
}