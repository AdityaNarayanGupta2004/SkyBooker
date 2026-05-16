//package com.skybooker.booking.service;
//
//import com.skybooker.booking.dto.BookingRequest;
//import com.skybooker.booking.dto.BookingResponse;
//import com.skybooker.booking.entity.Booking;
//import com.skybooker.booking.repository.BookingRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class BookingServiceImpl implements BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final RestTemplate restTemplate;
//
//    @Override
//    public BookingResponse bookFlight(BookingRequest request) {
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", request.getToken());
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//        // Step 1: Flight details fetch karo
//        String source = "", destination = "", depDate = "", depTime = "", airline = "";
//        try {
//            String flightUrl = "http://localhost:8082/flights/" + request.getFlightId();
//            ResponseEntity<Map> flightRes = restTemplate.exchange(
//                    flightUrl, HttpMethod.GET, entity, Map.class);
//            if (flightRes.getStatusCode().is2xxSuccessful() && flightRes.getBody() != null) {
//                Map body = flightRes.getBody();
//                source      = str(body.get("source"));
//                destination = str(body.get("destination"));
//                depDate     = str(body.get("departureDate"));
//                depTime     = str(body.get("departureTime"));
//                airline     = str(body.get("airline"));
//            }
//        } catch (Exception e) {
//            System.err.println("Could not fetch flight details: " + e.getMessage());
//        }
//
//        // Step 2: Reduce seats
//        String reduceUrl = "http://localhost:8082/flights/" + request.getFlightId()
//                + "/reduce-seats?seats=" + request.getSeats();
//        ResponseEntity<String> response = restTemplate.exchange(
//                reduceUrl, HttpMethod.PUT, entity, String.class);
//
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            throw new RuntimeException("Seat reduction failed: " + response.getStatusCode());
//        }
//
//        // Step 3: Booking save karo with flight details
//        Booking booking = new Booking();
//        booking.setFlightId(request.getFlightId());
//        booking.setUserEmail(request.getUserEmail());
//        booking.setSeatsBooked(request.getSeats());
//        booking.setTotalPrice(0.0);
//        booking.setStatus("CONFIRMED");
//        booking.setBookingTime(LocalDateTime.now());
//        booking.setSource(source);
//        booking.setDestination(destination);
//        booking.setDepartureDate(depDate);
//        booking.setDepartureTime(depTime);
//        booking.setAirline(airline);
//
//        Booking saved = bookingRepository.save(booking);
//
//        return new BookingResponse(
//                saved.getId(), "Booking successful", true,
//                source, destination, depDate, depTime, airline
//        );
//    }
//
//    @Override
//    public BookingResponse getBookingById(Long bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
//        return new BookingResponse(
//                booking.getId(), "Success", true,
//                booking.getSource(), booking.getDestination(),
//                booking.getDepartureDate(), booking.getDepartureTime(),
//                booking.getAirline()
//        );
//    }
//
//    private String str(Object o) {
//        return o != null ? o.toString() : "";
//    }
//}

package com.skybooker.booking.service;

import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    @Override
    public BookingResponse bookFlight(BookingRequest request) {
        log.info("Booking request — flightId: {}, user: {}, seats: {}",
                request.getFlightId(), request.getUserEmail(), request.getSeats());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", request.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String source = "";
        String destination = "";
        String depDate = "";
        String depTime = "";
        String airline = "";
        try {
            String flightUrl = "http://localhost:8082/flights/" + request.getFlightId();
            ResponseEntity<Map<String, Object>> flightRes = restTemplate.exchange(
                    flightUrl, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (flightRes.getStatusCode().is2xxSuccessful() && flightRes.getBody() != null) {
                Map<String, Object> body = flightRes.getBody();
                source      = str(body.get("source"));
                destination = str(body.get("destination"));
                depDate     = str(body.get("departureDate"));
                depTime     = str(body.get("departureTime"));
                airline     = str(body.get("airline"));
                log.info("Flight details fetched — {} → {}, date: {}", source, destination, depDate);
            }
        } catch (Exception e) {
            log.error("Could not fetch flight details for flightId: {} — {}", request.getFlightId(), e.getMessage());
        }

        String reduceUrl = "http://localhost:8082/flights/" + request.getFlightId()
                + "/reduce-seats?seats=" + request.getSeats();
        ResponseEntity<String> response = restTemplate.exchange(
                reduceUrl, HttpMethod.PUT, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Seat reduction failed — flightId: {}, status: {}",
                    request.getFlightId(), response.getStatusCode());
            throw new IllegalStateException("Seat reduction failed: " + response.getStatusCode());
        }

        Booking booking = new Booking();
        booking.setFlightId(request.getFlightId());
        booking.setUserEmail(request.getUserEmail());
        booking.setSeatsBooked(request.getSeats());
        booking.setTotalPrice(0.0);
        booking.setStatus("CONFIRMED");
        booking.setBookingTime(LocalDateTime.now());
        booking.setSource(source);
        booking.setDestination(destination);
        booking.setDepartureDate(depDate);
        booking.setDepartureTime(depTime);
        booking.setAirline(airline);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking saved — bookingId: {}, user: {}, route: {} → {}",
                saved.getId(), request.getUserEmail(), source, destination);

        return new BookingResponse(saved.getId(), "Booking successful", true,
                source, destination, depDate, depTime, airline);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        log.debug("Fetching booking — bookingId: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found — bookingId: {}", bookingId);
                    return new IllegalArgumentException("Booking not found: " + bookingId);
                });
        return new BookingResponse(booking.getId(), "Success", true,
                booking.getSource(), booking.getDestination(),
                booking.getDepartureDate(), booking.getDepartureTime(),
                booking.getAirline());
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}