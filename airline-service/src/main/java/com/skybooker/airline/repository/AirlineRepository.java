package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirlineRepository extends JpaRepository<Airline, Long> {

    Optional<Airline> findByIataCode(String iataCode);

    List<Airline> findByIsActive(boolean isActive);

    boolean existsByIataCode(String iataCode);
}
