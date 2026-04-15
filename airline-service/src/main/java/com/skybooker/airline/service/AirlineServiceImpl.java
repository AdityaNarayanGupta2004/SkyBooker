package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;

    // ==================== AIRLINE OPERATIONS ====================

    @Override
    public AirlineResponse addAirline(AirlineRequest request) {

        // same IATA code already exist toh nahi karta
        if (airlineRepository.existsByIataCode(request.getIataCode())) {
            throw new RuntimeException("Airline with IATA code " + request.getIataCode() + " already exists");
        }

        Airline airline = new Airline();
        airline.setName(request.getName());
        airline.setIataCode(request.getIataCode().toUpperCase());
        airline.setIcaoCode(request.getIcaoCode() != null ? request.getIcaoCode().toUpperCase() : null);
        airline.setCountry(request.getCountry());
        airline.setContactEmail(request.getContactEmail());
        airline.setContactPhone(request.getContactPhone());
        airline.setActive(true);

        Airline saved = airlineRepository.save(airline);
        return mapAirlineToResponse(saved, "Airline added successfully");
    }

    @Override
    public AirlineResponse getAirlineById(Long id) {
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Airline not found with id: " + id));
        return mapAirlineToResponse(airline, "Success");
    }

    @Override
    public AirlineResponse getAirlineByIata(String iataCode) {
        Airline airline = airlineRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Airline not found with IATA code: " + iataCode));
        return mapAirlineToResponse(airline, "Success");
    }

    @Override
    public List<AirlineResponse> getAllAirlines() {
        return airlineRepository.findAll()
                .stream()
                .map(a -> mapAirlineToResponse(a, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public List<AirlineResponse> getActiveAirlines() {
        return airlineRepository.findByIsActive(true)
                .stream()
                .map(a -> mapAirlineToResponse(a, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public AirlineResponse updateAirline(Long id, AirlineRequest request) {
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Airline not found with id: " + id));

        airline.setName(request.getName());
        airline.setCountry(request.getCountry());
        airline.setContactEmail(request.getContactEmail());
        airline.setContactPhone(request.getContactPhone());

        Airline updated = airlineRepository.save(airline);
        return mapAirlineToResponse(updated, "Airline updated successfully");
    }

    @Override
    public AirlineResponse toggleAirlineStatus(Long id) {
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Airline not found with id: " + id));

        // active hai toh deactivate karo, deactive hai toh activate karo
        airline.setActive(!airline.isActive());

        Airline updated = airlineRepository.save(airline);
        String msg = updated.isActive() ? "Airline activated successfully" : "Airline deactivated successfully";
        return mapAirlineToResponse(updated, msg);
    }

    // ==================== AIRPORT OPERATIONS ====================

    @Override
    public AirportResponse addAirport(AirportRequest request) {

        if (airportRepository.existsByIataCode(request.getIataCode())) {
            throw new RuntimeException("Airport with IATA code " + request.getIataCode() + " already exists");
        }

        Airport airport = new Airport();
        airport.setName(request.getName());
        airport.setIataCode(request.getIataCode().toUpperCase());
        airport.setIcaoCode(request.getIcaoCode() != null ? request.getIcaoCode().toUpperCase() : null);
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
        airport.setLatitude(request.getLatitude());
        airport.setLongitude(request.getLongitude());
        airport.setTimezone(request.getTimezone());

        Airport saved = airportRepository.save(airport);
        return mapAirportToResponse(saved, "Airport added successfully");
    }

    @Override
    public AirportResponse getAirportById(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Airport not found with id: " + id));
        return mapAirportToResponse(airport, "Success");
    }

    @Override
    public AirportResponse getAirportByIata(String iataCode) {
        Airport airport = airportRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Airport not found with IATA code: " + iataCode));
        return mapAirportToResponse(airport, "Success");
    }

    @Override
    public List<AirportResponse> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(a -> mapAirportToResponse(a, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public List<AirportResponse> searchAirports(String keyword) {
        // city ya airport name se search - flight search form autocomplete ke liye
        return airportRepository
                .findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(a -> mapAirportToResponse(a, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public AirportResponse updateAirport(Long id, AirportRequest request) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Airport not found with id: " + id));

        airport.setName(request.getName());
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
        airport.setLatitude(request.getLatitude());
        airport.setLongitude(request.getLongitude());
        airport.setTimezone(request.getTimezone());

        Airport updated = airportRepository.save(airport);
        return mapAirportToResponse(updated, "Airport updated successfully");
    }

    // ==================== MAPPERS ====================

    private AirlineResponse mapAirlineToResponse(Airline airline, String message) {
        AirlineResponse res = new AirlineResponse();
        res.setId(airline.getId());
        res.setName(airline.getName());
        res.setIataCode(airline.getIataCode());
        res.setIcaoCode(airline.getIcaoCode());
        res.setCountry(airline.getCountry());
        res.setContactEmail(airline.getContactEmail());
        res.setContactPhone(airline.getContactPhone());
        res.setActive(airline.isActive());
        res.setMessage(message);
        return res;
    }

    private AirportResponse mapAirportToResponse(Airport airport, String message) {
        AirportResponse res = new AirportResponse();
        res.setId(airport.getId());
        res.setName(airport.getName());
        res.setIataCode(airport.getIataCode());
        res.setIcaoCode(airport.getIcaoCode());
        res.setCity(airport.getCity());
        res.setCountry(airport.getCountry());
        res.setLatitude(airport.getLatitude());
        res.setLongitude(airport.getLongitude());
        res.setTimezone(airport.getTimezone());
        res.setMessage(message);
        return res;
    }
}
