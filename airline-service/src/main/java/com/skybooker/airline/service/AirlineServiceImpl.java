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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;

    // ==================== AIRLINE OPERATIONS ====================

    @Override
    public AirlineResponse addAirline(AirlineRequest request) {
        log.info("Adding airline — name: {}, IATA: {}", request.getName(), request.getIataCode());

        if (airlineRepository.existsByIataCode(request.getIataCode())) {
            log.warn("Airline add rejected — IATA code already exists: {}", request.getIataCode());
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
        log.info("Airline added — ID: {}, name: {}, IATA: {}", saved.getId(), saved.getName(), saved.getIataCode());
        return mapAirlineToResponse(saved, "Airline added successfully");
    }

    @Override
    public AirlineResponse getAirlineById(Long id) {
        log.debug("Fetching airline — id: {}", id);
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Airline not found — id: {}", id);
                    return new RuntimeException("Airline not found with id: " + id);
                });
        return mapAirlineToResponse(airline, "Success");
    }

    @Override
    public AirlineResponse getAirlineByIata(String iataCode) {
        log.debug("Fetching airline by IATA: {}", iataCode);
        Airline airline = airlineRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> {
                    log.warn("Airline not found — IATA: {}", iataCode);
                    return new RuntimeException("Airline not found with IATA code: " + iataCode);
                });
        return mapAirlineToResponse(airline, "Success");
    }

    @Override
    public List<AirlineResponse> getAllAirlines() {
        log.debug("Fetching all airlines");
        List<AirlineResponse> list = airlineRepository.findAll()
                .stream().map(a -> mapAirlineToResponse(a, "Success")).collect(Collectors.toList());
        log.debug("Total airlines found: {}", list.size());
        return list;
    }

    @Override
    public List<AirlineResponse> getActiveAirlines() {
        log.debug("Fetching active airlines");
        return airlineRepository.findByIsActive(true)
                .stream().map(a -> mapAirlineToResponse(a, "Success")).collect(Collectors.toList());
    }

    @Override
    public AirlineResponse updateAirline(Long id, AirlineRequest request) {
        log.info("Updating airline — id: {}", id);
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed — airline not found: {}", id);
                    return new RuntimeException("Airline not found with id: " + id);
                });

        airline.setName(request.getName());
        airline.setCountry(request.getCountry());
        airline.setContactEmail(request.getContactEmail());
        airline.setContactPhone(request.getContactPhone());

        Airline updated = airlineRepository.save(airline);
        log.info("Airline updated — id: {}, name: {}", id, updated.getName());
        return mapAirlineToResponse(updated, "Airline updated successfully");
    }

    @Override
    public AirlineResponse toggleAirlineStatus(Long id) {
        log.info("Toggle status request — airlineId: {}", id);
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Toggle failed — airline not found: {}", id);
                    return new RuntimeException("Airline not found with id: " + id);
                });

        airline.setActive(!airline.isActive());
        Airline updated = airlineRepository.save(airline);
        String status = updated.isActive() ? "ACTIVE" : "INACTIVE";
        log.info("Airline status changed — id: {}, new status: {}", id, status);

        String msg = updated.isActive() ? "Airline activated successfully" : "Airline deactivated successfully";
        return mapAirlineToResponse(updated, msg);
    }

    // ==================== AIRPORT OPERATIONS ====================

    @Override
    public AirportResponse addAirport(AirportRequest request) {
        log.info("Adding airport — name: {}, IATA: {}, city: {}",
                request.getName(), request.getIataCode(), request.getCity());

        if (airportRepository.existsByIataCode(request.getIataCode())) {
            log.warn("Airport add rejected — IATA code already exists: {}", request.getIataCode());
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
        log.info("Airport added — ID: {}, name: {}, city: {}", saved.getId(), saved.getName(), saved.getCity());
        return mapAirportToResponse(saved, "Airport added successfully");
    }

    @Override
    public AirportResponse getAirportById(Long id) {
        log.debug("Fetching airport — id: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Airport not found — id: {}", id);
                    return new RuntimeException("Airport not found with id: " + id);
                });
        return mapAirportToResponse(airport, "Success");
    }

    @Override
    public AirportResponse getAirportByIata(String iataCode) {
        log.debug("Fetching airport by IATA: {}", iataCode);
        Airport airport = airportRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> {
                    log.warn("Airport not found — IATA: {}", iataCode);
                    return new RuntimeException("Airport not found with IATA code: " + iataCode);
                });
        return mapAirportToResponse(airport, "Success");
    }

    @Override
    public List<AirportResponse> getAllAirports() {
        log.debug("Fetching all airports");
        return airportRepository.findAll()
                .stream().map(a -> mapAirportToResponse(a, "Success")).collect(Collectors.toList());
    }

    @Override
    public List<AirportResponse> searchAirports(String keyword) {
        log.info("Searching airports — keyword: {}", keyword);
        List<AirportResponse> results = airportRepository
                .findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword)
                .stream().map(a -> mapAirportToResponse(a, "Success")).collect(Collectors.toList());
        log.info("Airport search results — {} found for keyword: {}", results.size(), keyword);
        return results;
    }

    @Override
    public AirportResponse updateAirport(Long id, AirportRequest request) {
        log.info("Updating airport — id: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed — airport not found: {}", id);
                    return new RuntimeException("Airport not found with id: " + id);
                });

        airport.setName(request.getName());
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
        airport.setLatitude(request.getLatitude());
        airport.setLongitude(request.getLongitude());
        airport.setTimezone(request.getTimezone());

        Airport updated = airportRepository.save(airport);
        log.info("Airport updated — id: {}, name: {}", id, updated.getName());
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