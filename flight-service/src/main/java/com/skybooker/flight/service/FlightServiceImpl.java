package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final RestTemplate restTemplate;

    private static final String SEAT_SERVICE_URL = "http://localhost:8086/seats";
    private static final String JWT_SECRET = "my-super-secret-key-my-super-secret-key-12345";

    @Override
    public FlightResponse addFlight(FlightRequest request) {

        // ── DATE VALIDATION ───────────────────────────────────────
        LocalDate today = LocalDate.now();

        // Departure date past me nhihogi
        if (request.getDepartureDate() == null) {
            throw new RuntimeException("Departure date is required.");
        }
        if (request.getDepartureDate().isBefore(today)) {
            throw new RuntimeException(
                    "Departure date cannot be in the past. " +
                            "Please select today or a future date. (Today: " + today + ")"
            );
        }

        // Arrival date set karne liye hi
        LocalDate arrivalDate = request.getArrivalDate() != null
                ? request.getArrivalDate()
                : request.getDepartureDate();

        // Arrival date departure date se phale nhi ho skti
        if (arrivalDate.isBefore(request.getDepartureDate())) {
            throw new RuntimeException(
                    "Arrival date cannot be before departure date."
            );
        }

        // Same day flight mein arrival time departure time ke baad honi chahiye
        if (arrivalDate.equals(request.getDepartureDate())) {
            if (request.getDepartureTime() != null && request.getArrivalTime() != null) {
                if (request.getArrivalTime().compareTo(request.getDepartureTime()) <= 0) {
                    throw new RuntimeException(
                            "For same-day flights: arrival time must be after departure time. " +
                                    "For overnight flights: set a different arrival date."
                    );
                }
            }
        }

        // ── FLIGHT SAVE ───────────────────────────────────────────
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

        // Seats auto-generate hogi yha se
        autoGenerateSeats(saved.getId(), request.getTotalSeats());

        return mapToResponse(saved);
    }

    // ── AUTO SEAT GENERATION (same as before) ──────────────────
    private void autoGenerateSeats(Long flightId, int totalSeats) {

        String[] columns     = {"A", "B", "C", "D", "E", "F"};
        int      seatsPerRow = columns.length;
        int      totalRows   = (int) Math.ceil((double) totalSeats / seatsPerRow);

        String internalToken = buildInternalJwt();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(internalToken);

        int seatsCreated = 0;

        for (int row = 1; row <= totalRows && seatsCreated < totalSeats; row++) {
            for (int colIdx = 0; colIdx < columns.length && seatsCreated < totalSeats; colIdx++) {

                String col        = columns[colIdx];
                String seatNumber = row + col;

                String seatClass;
                if      (row <= 2) seatClass = "FIRST";
                else if (row <= 6) seatClass = "BUSINESS";
                else               seatClass = "ECONOMY";

                boolean isWindow    = col.equals("A") || col.equals("F");
                boolean isAisle     = col.equals("C") || col.equals("D");
                boolean hasExtraLeg = (row == 1) || (row == 7);

                double priceMultiplier;
                if      (seatClass.equals("FIRST"))    priceMultiplier = 3.0;
                else if (seatClass.equals("BUSINESS")) priceMultiplier = 2.0;
                else                                   priceMultiplier = 1.0;

                Map<String, Object> seatReq = new HashMap<>();
                seatReq.put("flightId",       flightId);
                seatReq.put("seatNumber",      seatNumber);
                seatReq.put("seatClass",       seatClass);
                seatReq.put("row",             row);
                seatReq.put("column",          col);
                seatReq.put("isWindow",        isWindow);
                seatReq.put("isAisle",         isAisle);
                seatReq.put("hasExtraLegroom", hasExtraLeg);
                seatReq.put("priceMultiplier", priceMultiplier);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(seatReq, headers);

                try {
                    restTemplate.postForObject(SEAT_SERVICE_URL, entity, Object.class);
                    seatsCreated++;
                } catch (Exception e) {
                    System.err.println("Seat auto-generate FAILED for seat " + seatNumber
                            + " on flight " + flightId + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Auto-generated " + seatsCreated + "/" + totalSeats
                + " seats for flight ID: " + flightId);
    }

    private String buildInternalJwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        return Jwts.builder()
                .setSubject("flight-service-internal")
                .claim("role", "AIRLINE_STAFF")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
                .signWith(key)
                .compact();
    }

    @Override
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightResponse> searchFlights(String source, String destination, LocalDate date) {
        return flightRepository
                .findBySourceAndDestinationAndDepartureDate(source, destination, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String reduceSeats(Long id, int seats) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        if (flight.getAvailableSeats() < seats) {
            throw new RuntimeException("Not enough seats available");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        flightRepository.save(flight);
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