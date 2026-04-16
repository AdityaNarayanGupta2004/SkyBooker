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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final RestTemplate restTemplate;

    // ============================================================
    //  SEAT-SERVICE ka URL — application.properties se bhi le sakte ho
    // ============================================================
    private static final String SEAT_SERVICE_URL = "http://localhost:8086/seats";

    // Auth-service ke saath SAME secret key honi chahiye
    private static final String JWT_SECRET = "my-super-secret-key-my-super-secret-key-12345";

    // ============================================================
    //  FLIGHT ADD — seats bhi auto-generate hongi
    // ============================================================
    @Override
    public FlightResponse addFlight(FlightRequest request) {

        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureDate(request.getDepartureDate());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(request.getTotalSeats());
        flight.setPrice(request.getPrice());
        flight.setCreatedAt(LocalDateTime.now());
        flight.setUpdatedAt(LocalDateTime.now());

        Flight saved = flightRepository.save(flight);

        // ✅ Flight save hone ke baad AUTOMATICALLY seats generate karo
        autoGenerateSeats(saved.getId(), request.getTotalSeats());

        return mapToResponse(saved);
    }

    // ============================================================
    //  AUTO SEAT GENERATION LOGIC
    //  Layout: 6 seats per row → A B C | D E F
    //  Row 1-2   → FIRST CLASS    (priceMultiplier 3.0)
    //  Row 3-6   → BUSINESS CLASS (priceMultiplier 2.0)
    //  Row 7+    → ECONOMY CLASS  (priceMultiplier 1.0)
    // ============================================================
    private void autoGenerateSeats(Long flightId, int totalSeats) {

        String[] columns     = {"A", "B", "C", "D", "E", "F"};
        int      seatsPerRow = columns.length;
        int      totalRows   = (int) Math.ceil((double) totalSeats / seatsPerRow);

        // Internal service-to-service call ke liye ADMIN token banao
        String internalToken = buildInternalJwt();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(internalToken);   // seat-service ko AIRLINE_STAFF/ADMIN chahiye

        int seatsCreated = 0;

        for (int row = 1; row <= totalRows && seatsCreated < totalSeats; row++) {
            for (int colIdx = 0; colIdx < columns.length && seatsCreated < totalSeats; colIdx++) {

                String col        = columns[colIdx];
                String seatNumber = row + col;      // e.g. "1A", "12C"

                // ---- Class determine karo ----
                String seatClass;
                if      (row <= 2) seatClass = "FIRST";
                else if (row <= 6) seatClass = "BUSINESS";
                else               seatClass = "ECONOMY";

                // ---- Seat properties ----
                boolean isWindow      = col.equals("A") || col.equals("F");
                boolean isAisle       = col.equals("C") || col.equals("D");
                boolean hasExtraLeg   = (row == 1) || (row == 7); // first row & emergency exit row

                double priceMultiplier;
                if      (seatClass.equals("FIRST"))    priceMultiplier = 3.0;
                else if (seatClass.equals("BUSINESS")) priceMultiplier = 2.0;
                else                                   priceMultiplier = 1.0;

                // ---- Request body banao ----
                Map<String, Object> seatReq = new HashMap<>();
                seatReq.put("flightId",         flightId);
                seatReq.put("seatNumber",        seatNumber);
                seatReq.put("seatClass",         seatClass);
                seatReq.put("row",               row);
                seatReq.put("column",            col);
                seatReq.put("isWindow",          isWindow);
                seatReq.put("isAisle",           isAisle);
                seatReq.put("hasExtraLegroom",   hasExtraLeg);
                seatReq.put("priceMultiplier",   priceMultiplier);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(seatReq, headers);

                try {
                    restTemplate.postForObject(SEAT_SERVICE_URL, entity, Object.class);
                    seatsCreated++;
                } catch (Exception e) {
                    // Ek seat fail ho toh poori flight block mat karo — sirf log karo
                    System.err.println("⚠️  Seat auto-generate FAILED for seat " + seatNumber
                            + " on flight " + flightId + " → " + e.getMessage());
                }
            }
        }

        System.out.println("✅ Auto-generated " + seatsCreated + "/" + totalSeats
                + " seats for flight ID: " + flightId);
    }

    // ============================================================
    //  Internal JWT — flight-service seat-service ko call karte waqt
    //  khud ko AIRLINE_STAFF ke roop mein identify karta hai.
    //  Ye token sirf internal use ke liye hai, user ko nahi milta.
    // ============================================================
    private String buildInternalJwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        return Jwts.builder()
                .setSubject("flight-service-internal")
                .claim("role", "AIRLINE_STAFF")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5)) // 5 min
                .signWith(key)
                .compact();
    }

    // ============================================================
    //  GET ALL FLIGHTS
    // ============================================================
    @Override
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  SEARCH FLIGHTS
    // ============================================================
    @Override
    public List<FlightResponse> searchFlights(String source, String destination,
                                              java.time.LocalDate date) {
        return flightRepository
                .findBySourceAndDestinationAndDepartureDate(source, destination, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  REDUCE SEATS
    // ============================================================
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

    // ============================================================
    //  HELPER
    // ============================================================
    private FlightResponse mapToResponse(Flight flight) {
        FlightResponse res = new FlightResponse();
        res.setId(flight.getId());
        res.setFlightNumber(flight.getFlightNumber());
        res.setAirline(flight.getAirline());
        res.setSource(flight.getSource());
        res.setDestination(flight.getDestination());
        res.setDepartureDate(flight.getDepartureDate());
        res.setDepartureTime(flight.getDepartureTime());
        res.setArrivalTime(flight.getArrivalTime());
        res.setAvailableSeats(flight.getAvailableSeats());
        res.setPrice(flight.getPrice());
        return res;
    }
}






//package com.skybooker.flight.service;
//
//import com.skybooker.flight.dto.FlightRequest;
//import com.skybooker.flight.dto.FlightResponse;
//import com.skybooker.flight.entity.Flight;
//import com.skybooker.flight.repository.FlightRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class FlightServiceImpl implements FlightService {
//
//    private final FlightRepository flightRepository;
//
//    @Override
//    public FlightResponse addFlight(FlightRequest request) {
//
//        Flight flight = new Flight();
//        flight.setFlightNumber(request.getFlightNumber());
//        flight.setAirline(request.getAirline());
//        flight.setSource(request.getSource());
//        flight.setDestination(request.getDestination());
//        flight.setDepartureDate(request.getDepartureDate());
//        flight.setDepartureTime(request.getDepartureTime());
//        flight.setArrivalTime(request.getArrivalTime());
//        flight.setTotalSeats(request.getTotalSeats());
//        flight.setAvailableSeats(request.getTotalSeats());
//        flight.setPrice(request.getPrice());
//
//        flight.setCreatedAt(LocalDateTime.now());
//        flight.setUpdatedAt(LocalDateTime.now());
//
//        Flight saved = flightRepository.save(flight);
//
//        return mapToResponse(saved);
//    }
//
//    @Override
//    public List<FlightResponse> getAllFlights() {
//        return flightRepository.findAll()
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<FlightResponse> searchFlights(String source, String destination, java.time.LocalDate date) {
//
//        return flightRepository
//                .findBySourceAndDestinationAndDepartureDate(source, destination, date)
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    private FlightResponse mapToResponse(Flight flight) {
//        FlightResponse res = new FlightResponse();
//        res.setId(flight.getId());
//        res.setFlightNumber(flight.getFlightNumber());
//        res.setAirline(flight.getAirline());
//        res.setSource(flight.getSource());
//        res.setDestination(flight.getDestination());
//        res.setDepartureDate(flight.getDepartureDate());
//        res.setDepartureTime(flight.getDepartureTime());
//        res.setArrivalTime(flight.getArrivalTime());
//        res.setAvailableSeats(flight.getAvailableSeats());
//        res.setPrice(flight.getPrice());
//        return res;
//    }
//
//    @Override
//    public String reduceSeats(Long id, int seats){
//
//        Flight flight = flightRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Flight not found"));
//
//        if(flight.getAvailableSeats() < seats){
//            throw new RuntimeException("Not enough seats available");
//        }
//
//        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
//
//        flightRepository.save(flight);
//
//        return "Seats reduced successfully";
//    }
//}