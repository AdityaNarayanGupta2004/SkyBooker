package com.skybooker.booking.service;

import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    @Override
    public BookingResponse bookFlight(BookingRequest request) {

        // flight-service ka reduce-seats endpoint PUT hai, aur seats query param chahiye
        String url = "http://localhost:8082/flights/" + request.getFlightId()
                + "/reduce-seats?seats=" + request.getSeats();

        // Authorization header forward kar rahe hain flight-service ko
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", request.getToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,   // flight-service mein @PutMapping hai
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Seat reduction failed: " + response.getStatusCode());
        }

        // booking DB mein save karo
        Booking booking = new Booking();
        booking.setFlightId(request.getFlightId());
        booking.setUserEmail(request.getUserEmail());
        booking.setSeatsBooked(request.getSeats());
        booking.setTotalPrice(0.0); // price calculation baad mein add karenge
        booking.setStatus("CONFIRMED");
        booking.setBookingTime(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        return new BookingResponse(saved.getId(), "Booking successful", true);
    }
}
