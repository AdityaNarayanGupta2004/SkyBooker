package com.skybooker.flight.repository;

import com.skybooker.flight.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findBySourceAndDestinationAndDepartureDate(
            String source,
            String destination,
            LocalDate departureDate
    );
}