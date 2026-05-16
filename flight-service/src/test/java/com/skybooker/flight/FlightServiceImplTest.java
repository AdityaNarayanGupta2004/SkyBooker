package com.skybooker.flight;

import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.FlightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private RestTemplate restTemplate;

    private final String internalSecret = "my-new-secure-random-secret-key-for-skybooker-2026-flight";

    private FlightServiceImpl flightServiceImpl;

    @BeforeEach
    void setUp() {
        flightServiceImpl = new FlightServiceImpl(flightRepository, restTemplate, internalSecret);
    }

    private Flight banaoFlight() {
        Flight f = new Flight();
        f.setId(1L);
        f.setFlightNumber("6E-101");
        f.setAirline("IndiGo");
        f.setSource("DEL");
        f.setDestination("BOM");
        f.setDepartureDate(LocalDate.now().plusDays(5));
        f.setDepartureTime("10:00");
        f.setArrivalDate(LocalDate.now().plusDays(5));
        f.setArrivalTime("12:00");
        f.setTotalSeats(180);
        f.setAvailableSeats(180);
        f.setPrice(5000.0);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        return f;
    }

    @Test
    void getFlightById_WhenFlightExists_ShouldReturnFlight() {
        Flight flight = banaoFlight();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        FlightResponse res = flightServiceImpl.getFlightById(1L);

        assertNotNull(res);
        assertEquals("6E-101", res.getFlightNumber());
        assertEquals("DEL", res.getSource());
        assertEquals("BOM", res.getDestination());
    }

    @Test
    void getFlightById_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.getFlightById(99L));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    @Test
    void getAllFlights_ShouldReturnAllFlights() {
        Flight f1 = banaoFlight();
        Flight f2 = banaoFlight();
        f2.setId(2L);
        f2.setFlightNumber("AI-202");

        when(flightRepository.findAll()).thenReturn(List.of(f1, f2));

        List<FlightResponse> result = flightServiceImpl.getAllFlights();

        assertEquals(2, result.size());
    }

    @Test
    void getAllFlights_WhenNoFlights_ShouldReturnEmptyList() {
        when(flightRepository.findAll()).thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.getAllFlights();

        assertTrue(result.isEmpty());
    }

    @Test
    void searchFlights_WithValidParams_ShouldReturnMatchingFlights() {
        Flight flight = banaoFlight();
        LocalDate date = LocalDate.now().plusDays(5);

        when(flightRepository.findBySourceAndDestinationAndDepartureDate("DEL", "BOM", date))
                .thenReturn(List.of(flight));

        List<FlightResponse> result = flightServiceImpl.searchFlights("DEL", "BOM", date);

        assertEquals(1, result.size());
        assertEquals("DEL", result.get(0).getSource());
        assertEquals("BOM", result.get(0).getDestination());
    }

    @Test
    void searchFlights_WhenNoMatch_ShouldReturnEmptyList() {
        LocalDate date = LocalDate.now().plusDays(5);

        when(flightRepository.findBySourceAndDestinationAndDepartureDate("DEL", "CCU", date))
                .thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.searchFlights("DEL", "CCU", date);

        assertTrue(result.isEmpty());
    }

    @Test
    void reduceSeats_WhenEnoughSeatsAvailable_ShouldSucceed() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(50);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        String result = flightServiceImpl.reduceSeats(1L, 2);

        assertEquals("Seats reduced successfully", result);
        assertEquals(48, flight.getAvailableSeats());
    }

    @Test
    void reduceSeats_WhenNotEnoughSeats_ShouldThrowException() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(1);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(1L, 5));

        assertTrue(ex.getMessage().contains("Not enough seats"));
    }

    @Test
    void reduceSeats_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(999L, 1));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    @Test
    void addFlight_WithValidRequest_ShouldSucceed() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setFlightNumber("6E-505");
        req.setAirline("IndiGo");
        req.setSource("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.now().plusDays(1));
        req.setDepartureTime("10:00");
        req.setArrivalTime("12:00");
        req.setTotalSeats(10);
        req.setPrice(4000.0);

        Flight savedFlight = new Flight();
        savedFlight.setId(505L);
        savedFlight.setFlightNumber("6E-505");
        savedFlight.setTotalSeats(10);
        savedFlight.setAvailableSeats(10);

        when(flightRepository.save(any(Flight.class))).thenReturn(savedFlight);
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(new Object());

        FlightResponse res = flightServiceImpl.addFlight(req);

        assertNotNull(res);
        assertEquals(505L, res.getId());
        verify(flightRepository, times(1)).save(any());
        verify(restTemplate, times(10)).postForObject(anyString(), any(), any());
    }

    @Test
    void determineSeatClass_Coverage() {
        // Since determineSeatClass is private, we test it via addFlight or use Reflection
        // But we already test it via addFlight which loops through rows.
        // Let's add a test for a flight with 50 seats to cover all rows
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setFlightNumber("AI-100");
        req.setTotalSeats(50); 
        req.setDepartureDate(LocalDate.now().plusDays(1));
        req.setDepartureTime("12:00");

        Flight saved = new Flight();
        saved.setId(100L);
        saved.setTotalSeats(50);
        when(flightRepository.save(any())).thenReturn(saved);
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(new Object());

        flightServiceImpl.addFlight(req);
        // This covers rows 1 to 9 (First, Business, Economy)
        verify(flightRepository, atLeastOnce()).save(any());
    }

    @Test
    void addFlight_WithPastDate_ShouldThrowException() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setDepartureDate(LocalDate.now().minusDays(1));
        req.setDepartureTime("10:00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> flightServiceImpl.addFlight(req));
        
        assertTrue(ex.getMessage().contains("cannot be in the past"));
    }

    @Test
    void addFlight_WithArrivalBeforeDeparture_ShouldThrowException() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setDepartureDate(LocalDate.now().plusDays(1));
        req.setDepartureTime("10:00");
        req.setArrivalDate(LocalDate.now().plusDays(1));
        req.setArrivalTime("08:00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> flightServiceImpl.addFlight(req));
        
        assertTrue(ex.getMessage().contains("arrival time must be after departure time"));
    }

    @Test
    void addFlight_WithMissingDepartureDate_ShouldThrowException() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setDepartureDate(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> flightServiceImpl.addFlight(req));
        
        assertTrue(ex.getMessage().contains("Departure date is required"));
    }

    @Test
    void addFlight_WithInvalidTimeFormat_ShouldThrowException() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setDepartureDate(LocalDate.now());
        req.setDepartureTime("invalid-time");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> flightServiceImpl.addFlight(req));
        
        assertTrue(ex.getMessage().contains("Invalid departure time format"));
    }

    @Test
    void addFlight_WithSameDayPastTime_ShouldThrowException() {
        com.skybooker.flight.dto.FlightRequest req = new com.skybooker.flight.dto.FlightRequest();
        req.setDepartureDate(LocalDate.now());
        java.time.LocalTime pastTime = java.time.LocalTime.now().minusHours(1);
        req.setDepartureTime(pastTime.toString());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> flightServiceImpl.addFlight(req));
        
        assertTrue(ex.getMessage().contains("Departure time has already passed"));
    }
}
