package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final RestTemplate restTemplate;

    private static final String SEAT_SERVICE_URL = "http://localhost:8086/seats";
    private static final String JWT_SECRET = "my-super-secret-key-my-super-secret-key-12345";

    @Override
    public FlightResponse addFlight(FlightRequest request) {
        log.info("Adding flight — number: {}, route: {} → {}, date: {}",
                request.getFlightNumber(), request.getSource(),
                request.getDestination(), request.getDepartureDate());

        validateFlightRequest(request);

        LocalDate arrivalDate = request.getArrivalDate() != null
                ? request.getArrivalDate()
                : request.getDepartureDate();

        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureDate(request.getDepartureDate());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalDate(arrivalDate);
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(request.getTotalSeats());
        flight.setPrice(request.getPrice());
        flight.setCreatedAt(LocalDateTime.now());
        flight.setUpdatedAt(LocalDateTime.now());

        Flight saved = flightRepository.save(flight);
        log.info("Flight saved — ID: {}, number: {}", saved.getId(), saved.getFlightNumber());

        autoGenerateSeats(saved.getId(), request.getTotalSeats());
        return mapToResponse(saved);
    }

    private void validateFlightRequest(FlightRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (request.getDepartureDate() == null)
            throw new IllegalArgumentException("Departure date is required.");

        if (request.getDepartureTime() == null || request.getDepartureTime().isBlank())
            throw new IllegalArgumentException("Departure time is required.");

        if (request.getDepartureDate().isBefore(today)) {
            log.warn("Flight add rejected — departure date in past: {}", request.getDepartureDate());
            throw new IllegalArgumentException(
                    "Departure date cannot be in the past. " +
                            "Please select today or a future date. (Today: " + today + ")");
        }

        if (request.getDepartureDate().isEqual(today)) {
            LocalTime depTime;
            try {
                depTime = LocalTime.parse(request.getDepartureTime());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid departure time format. Use HH:mm (e.g. 14:30).");
            }
            if (!LocalDateTime.of(request.getDepartureDate(), depTime).isAfter(now)) {
                log.warn("Flight add rejected — departure time already passed: {}", request.getDepartureTime());
                throw new IllegalArgumentException(
                        "Departure time has already passed for today. " +
                                "Current time is " + now.toLocalTime().withSecond(0).withNano(0) +
                                ". Please select a future departure time or a future date.");
            }
        }

        LocalDate arrivalDate = request.getArrivalDate() != null
                ? request.getArrivalDate()
                : request.getDepartureDate();

        if (arrivalDate.isBefore(request.getDepartureDate()))
            throw new IllegalArgumentException("Arrival date cannot be before departure date.");

        if (arrivalDate.equals(request.getDepartureDate())
                && request.getDepartureTime() != null
                && request.getArrivalTime() != null
                && !request.getArrivalTime().isBlank()
                && request.getArrivalTime().compareTo(request.getDepartureTime()) <= 0)
            throw new IllegalArgumentException(
                    "For same-day flights: arrival time must be after departure time. " +
                            "For overnight flights: set a different arrival date.");
    }

    private void autoGenerateSeats(Long flightId, int totalSeats) {
        log.info("Auto-generating {} seats for flightId: {}", totalSeats, flightId);

        String[] columns = {"A", "B", "C", "D", "E", "F"};
        int totalRows    = (int) Math.ceil((double) totalSeats / columns.length);
        String token     = buildInternalJwt();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        int created = 0;

        for (int row = 1; row <= totalRows && created < totalSeats; row++) {
            for (int colIdx = 0; colIdx < columns.length && created < totalSeats; colIdx++) {

                String col    = columns[colIdx];
                String seatNo = row + col;

                String seatClass;
                if      (row <= 2) seatClass = "FIRST";
                else if (row <= 6) seatClass = "BUSINESS";
                else               seatClass = "ECONOMY";

                double multi;
                if      (seatClass.equals("FIRST"))    multi = 3.0;
                else if (seatClass.equals("BUSINESS")) multi = 2.0;
                else                                   multi = 1.0;

                Map<String, Object> body = new HashMap<>();
                body.put("flightId",        flightId);
                body.put("seatNumber",      seatNo);
                body.put("seatClass",       seatClass);
                body.put("row",             row);
                body.put("column",          col);
                body.put("window",          col.equals("A") || col.equals("F"));
                body.put("aisle",           col.equals("C") || col.equals("D"));
                body.put("hasExtraLegroom", row == 1 || row == 7);
                body.put("priceMultiplier", multi);

                try {
                    restTemplate.postForObject(
                            SEAT_SERVICE_URL,
                            new HttpEntity<>(body, headers),
                            Object.class
                    );
                    created++;
                } catch (Exception e) {
                    log.error("Seat auto-generate FAILED — seat: {}, flightId: {}, reason: {}",
                            seatNo, flightId, e.getMessage());
                }
            }
        }
        log.info("Seat auto-generation complete — {}/{} seats created for flightId: {}",
                created, totalSeats, flightId);
    }

    private String buildInternalJwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        return Jwts.builder()
                .setSubject("flight-service-internal")
                .claim("role", "AIRLINE_STAFF")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(key)
                .compact();
    }

    @Override
    public FlightResponse getFlightById(Long id) {
        log.debug("Fetching flight by ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Flight not found — ID: {}", id);
                    return new IllegalArgumentException("Flight not found with id: " + id);
                });
        return mapToResponse(flight);
    }

    @Override
    public List<FlightResponse> getAllFlights() {
        log.debug("Fetching all flights");
        return flightRepository.findAll().stream()
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<FlightResponse> searchFlights(String source, String destination, LocalDate date) {
        log.info("Searching flights — {} → {}, date: {}", source, destination, date);
        List<FlightResponse> results = flightRepository
                .findBySourceAndDestinationAndDepartureDate(source, destination, date)
                .stream().map(this::mapToResponse).toList();
        log.info("Search results — {} flight(s) found", results.size());
        return results;
    }

    @Override
    public String reduceSeats(Long id, int seats) {
        log.info("Reducing {} seat(s) for flightId: {}", seats, id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("reduceSeats — flight not found: {}", id);
                    return new IllegalArgumentException("Flight not found");
                });
        if (flight.getAvailableSeats() < seats) {
            log.warn("reduceSeats — not enough seats. available: {}, requested: {}",
                    flight.getAvailableSeats(), seats);
            throw new IllegalStateException("Not enough seats available");
        }
        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        flightRepository.save(flight);
        log.info("Seats reduced — flightId: {}, remaining: {}", id, flight.getAvailableSeats());
        return "Seats reduced successfully";
    }

    private FlightResponse mapToResponse(Flight flight) {
        FlightResponse res = new FlightResponse();
        res.setId(flight.getId());
        res.setFlightNumber(flight.getFlightNumber());
        res.setAirline(flight.getAirline());
        res.setSource(flight.getSource());
        res.setDestination(flight.getDestination());
        res.setDepartureDate(flight.getDepartureDate());
        res.setDepartureTime(flight.getDepartureTime());
        res.setArrivalDate(flight.getArrivalDate());
        res.setArrivalTime(flight.getArrivalTime());
        res.setAvailableSeats(flight.getAvailableSeats());
        res.setPrice(flight.getPrice());
        return res;
    }
}