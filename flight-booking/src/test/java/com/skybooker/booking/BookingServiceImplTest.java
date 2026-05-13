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

import java.time.LocalDateTime;
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

    // RestTemplate mock karo — flight-service HTTP call karta hai
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BookingServiceImpl bookingServiceImpl;

    // Helper — ready-made Booking banao
    private Booking banaoBooking() {
        Booking b = new Booking();
        b.setId(1L);
        b.setFlightId(10L);
        b.setUserEmail("rahul@gmail.com");
        b.setSeatsBooked(2);
        b.setTotalPrice(0.0);
        b.setStatus("CONFIRMED");
        b.setBookingTime(LocalDateTime.now());
        b.setSource("Delhi");
        b.setDestination("Mumbai");
        b.setDepartureDate("2025-12-01");
        b.setDepartureTime("10:00");
        b.setAirline("IndiGo");
        return b;
    }

    // Helper — ready-made BookingRequest banao
    private BookingRequest banaoRequest() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        req.setUserEmail("rahul@gmail.com");
        req.setSeats(2);
        req.setToken("Bearer fake-jwt-token");
        return req;
    }

    // Helper — flight details ka fake Map banao
    private Map<String, Object> banaoFlightDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("source", "Delhi");
        map.put("destination", "Mumbai");
        map.put("departureDate", "2025-12-01");
        map.put("departureTime", "10:00");
        map.put("airline", "IndiGo");
        return map;
    }

    // ---------------------------------------------------------------
    // BOOK FLIGHT TESTS
    // ---------------------------------------------------------------

    // Test 1: Booking successfully ho jaye with flight details
    @Test
    void bookFlight_WhenSuccessful_ShouldReturnBookingWithFlightDetails() {
        BookingRequest req = banaoRequest();
        Booking saved = banaoBooking();

        // Flight details ka mock response
        ResponseEntity<Map> flightResponse = new ResponseEntity<>(banaoFlightDetails(), HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(flightResponse);

        // Reduce seats ka mock response
        ResponseEntity<String> reduceResponse = new ResponseEntity<>("Seats reduced successfully", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(reduceResponse);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse res = bookingServiceImpl.bookFlight(req);

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals("Delhi", res.getSource());
        assertEquals("Mumbai", res.getDestination());
        assertEquals("IndiGo", res.getAirline());
        assertEquals("Booking successful", res.getMessage());
    }

    // Test 2: Flight details fetch fail ho toh bhi booking ho jaye (graceful degradation)
    @Test
    void bookFlight_WhenFlightDetailsFetchFails_ShouldStillBookWithEmptyDetails() {
        BookingRequest req = banaoRequest();
        Booking saved = banaoBooking();
        saved.setSource("");
        saved.setDestination("");
        saved.setAirline("");

        // Flight GET call fail karo
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Flight service down"));

        // Reduce seats ka mock response
        ResponseEntity<String> reduceResponse = new ResponseEntity<>("Seats reduced successfully", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(reduceResponse);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        // Exception nahi aana chahiye — booking ho jani chahiye
        BookingResponse res = bookingServiceImpl.bookFlight(req);

        assertNotNull(res);
        assertTrue(res.isSuccess());
    }

    // Test 3: Seat reduce fail ho toh booking fail ho
    @Test
    void bookFlight_WhenSeatReductionFails_ShouldThrowException() {
        BookingRequest req = banaoRequest();

        // Flight GET call success
        ResponseEntity<Map> flightResponse = new ResponseEntity<>(banaoFlightDetails(), HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(flightResponse);

        // Reduce seats fail karo
        ResponseEntity<String> failResponse = new ResponseEntity<>("Not enough seats", HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(failResponse);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingServiceImpl.bookFlight(req));

        assertTrue(ex.getMessage().contains("Seat reduction failed"));
        // Save bilkul call nahi hona chahiye
        verify(bookingRepository, never()).save(any());
    }

    // Test 4: Booking save hone ke baad bookingId response mein aaye
    @Test
    void bookFlight_ShouldReturnBookingIdAfterSave() {
        BookingRequest req = banaoRequest();
        Booking saved = banaoBooking();
        saved.setId(42L);

        ResponseEntity<Map> flightResponse = new ResponseEntity<>(banaoFlightDetails(), HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(flightResponse);

        ResponseEntity<String> reduceResponse = new ResponseEntity<>("Seats reduced successfully", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(reduceResponse);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse res = bookingServiceImpl.bookFlight(req);

        assertEquals(42L, res.getBookingId());
    }

    // ---------------------------------------------------------------
    // GET BOOKING BY ID TESTS
    // ---------------------------------------------------------------

    // Test 5: BookingId se booking mile
    @Test
    void getBookingById_WhenExists_ShouldReturnBooking() {
        Booking booking = banaoBooking();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingResponse res = bookingServiceImpl.getBookingById(1L);

        assertNotNull(res);
        assertEquals(1L, res.getBookingId());
        assertEquals("Delhi", res.getSource());
        assertEquals("Mumbai", res.getDestination());
        assertEquals("IndiGo", res.getAirline());
        assertTrue(res.isSuccess());
    }

    // Test 6: Galat bookingId se exception aaye
    @Test
    void getBookingById_WhenNotFound_ShouldThrowException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingServiceImpl.getBookingById(999L));

        assertTrue(ex.getMessage().contains("Booking not found"));
    }

    // Test 7: Booking ka status CONFIRMED hona chahiye
    @Test
    void bookFlight_SavedBooking_ShouldHaveConfirmedStatus() {
        BookingRequest req = banaoRequest();
        Booking saved = banaoBooking();

        ResponseEntity<Map> flightResponse = new ResponseEntity<>(banaoFlightDetails(), HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(flightResponse);

        ResponseEntity<String> reduceResponse = new ResponseEntity<>("Seats reduced successfully", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(reduceResponse);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        bookingServiceImpl.bookFlight(req);

        // Verify karo ki save ke time CONFIRMED status tha
        verify(bookingRepository).save(argThat(b -> "CONFIRMED".equals(b.getStatus())));
    }

    // Test 8: Booking mein sahi flightId save ho
    @Test
    void bookFlight_ShouldSaveCorrectFlightId() {
        BookingRequest req = banaoRequest();
        Booking saved = banaoBooking();

        ResponseEntity<Map> flightResponse = new ResponseEntity<>(banaoFlightDetails(), HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/flights/10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(flightResponse);

        ResponseEntity<String> reduceResponse = new ResponseEntity<>("Seats reduced successfully", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/reduce-seats"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(reduceResponse);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        bookingServiceImpl.bookFlight(req);

        // Verify karo ki flightId 10 save hua
        verify(bookingRepository).save(argThat(b -> b.getFlightId().equals(10L)));
    }
}