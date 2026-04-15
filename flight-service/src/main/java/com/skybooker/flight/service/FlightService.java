package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;

import java.time.LocalDate;
import java.util.List;

public interface FlightService {

    FlightResponse addFlight(FlightRequest request);

    List<FlightResponse> getAllFlights();

    List<FlightResponse> searchFlights(String source, String destination, LocalDate date);

    String reduceSeats(Long id, int seats);
}