package com.skybooker.booking;

import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.repository.BookingRepository;
import com.skybooker.booking.service.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @SuppressWarnings("unchecked")
    void bookFlight_Success() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(1L);
        req.setSeats(2);
        req.setUserEmail("test@gmail.com");
        req.setToken("Bearer fake-token");

        Map<String, Object> flightData = new HashMap<>();
        flightData.put("source", "DEL");
        flightData.put("destination", "BOM");
        flightData.put("departureDate", "2026-05-20");

        // Mock Step 1: Fetch flight
        when(restTemplate.exchange(contains("/flights/1"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(flightData, HttpStatus.OK));

        // Mock Step 2: Reduce seats
        when(restTemplate.exchange(contains("/reduce-seats"), eq(HttpMethod.PUT), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Mock Step 3: Save booking
        when(bookingRepository.save(any())).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(50L);
            return b;
        });

        BookingResponse res = bookingService.bookFlight(req);
        assertEquals(50L, res.getBookingId());
        assertEquals("DEL", res.getSource());
    }

    @Test
    void bookFlight_ReduceSeatsFails_ThrowsException() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(1L);
        req.setSeats(2);

        // Mock Step 1: Fetch flight (optional for this test but good to have)
        when(restTemplate.exchange(contains("/flights/1"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

        // Mock Step 2: Reduce seats fails
        when(restTemplate.exchange(contains("/reduce-seats"), eq(HttpMethod.PUT), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST));

        assertThrows(IllegalStateException.class, () -> bookingService.bookFlight(req));
    }

    @Test
    void getBookingById_Success() {
        Booking b = new Booking();
        b.setId(50L);
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(b));
        assertEquals(50L, bookingService.getBookingById(50L).getBookingId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void bookFlight_FlightFetchFails_ShouldContinueWithEmptyDetails() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(1L);
        req.setSeats(1);
        req.setToken("Bearer token");

        // Mock Step 1: Fetch flight FAILS (throws exception)
        when(restTemplate.exchange(contains("/flights/1"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Service down"));

        // Mock Step 2: Reduce seats succeeds
        when(restTemplate.exchange(contains("/reduce-seats"), eq(HttpMethod.PUT), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Mock Step 3: Save booking
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingResponse res = bookingService.bookFlight(req);
        assertEquals("", res.getSource()); 
    }
}