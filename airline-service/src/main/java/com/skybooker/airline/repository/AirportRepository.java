package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByIataCode(String iataCode);

    List<Airport> findByCity(String city);

    List<Airport> findByCountry(String country);

    // flight search form mein autocomplete ke liye
    List<Airport> findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(String city, String name);

    boolean existsByIataCode(String iataCode);
}
