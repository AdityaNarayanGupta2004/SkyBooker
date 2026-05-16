package com.skybooker.flight;

import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.FlightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    // RestTemplate bhi mock karo — autoGenerateSeats HTTP call karta hai
    @Mock
    private RestTemplate restTemplate;

    private FlightServiceImpl flightServiceImpl;

    @BeforeEach
    void setUp() {
        flightServiceImpl = new FlightServiceImpl(flightRepository, restTemplate, "test-secret-key-must-be-long-enough-1234567890");
    }

    // Ek ready-made Flight entity banana ka helper
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



    // ---------------------------------------------------------------
    // GET FLIGHT BY ID TESTS
    // ---------------------------------------------------------------

    // Test 1: Sahi ID se flight mile
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

    // Test 2: Galat ID se exception aaye
    @Test
    void getFlightById_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.getFlightById(99L));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    // ---------------------------------------------------------------
    // GET ALL FLIGHTS TESTS
    // ---------------------------------------------------------------

    // Test 3: Saari flights milein
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

    // Test 4: Koi flight nahi hai toh empty list aaye
    @Test
    void getAllFlights_WhenNoFlights_ShouldReturnEmptyList() {
        when(flightRepository.findAll()).thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.getAllFlights();

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // SEARCH FLIGHTS TESTS
    // ---------------------------------------------------------------

    // Test 5: Source, destination aur date se flights milein
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

    // Test 6: Koi matching flight nahi ho toh empty list aaye
    @Test
    void searchFlights_WhenNoMatch_ShouldReturnEmptyList() {
        LocalDate date = LocalDate.now().plusDays(5);

        when(flightRepository.findBySourceAndDestinationAndDepartureDate("DEL", "CCU", date))
                .thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.searchFlights("DEL", "CCU", date);

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // REDUCE SEATS TESTS
    // ---------------------------------------------------------------

    // Test 7: Seats sahi se reduce ho
    @Test
    void reduceSeats_WhenEnoughSeatsAvailable_ShouldSucceed() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(50);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        String result = flightServiceImpl.reduceSeats(1L, 2);

        assertEquals("Seats reduced successfully", result);
        // 50 - 2 = 48 hone chahiye
        assertEquals(48, flight.getAvailableSeats());
    }

    // Test 8: Jitne seats maange utne available na ho toh exception aaye
    @Test
    void reduceSeats_WhenNotEnoughSeats_ShouldThrowException() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(1); // sirf 1 seat hai

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        // 5 seats mangna — impossible
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(1L, 5));

        assertTrue(ex.getMessage().contains("Not enough seats"));
    }

    // Test 9: Flight nahi mili toh seats reduce karne pe exception aaye
    @Test
    void reduceSeats_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(999L, 1));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    // Test 10: Price aur seats response mein sahi aayein
    @Test
    void getFlightById_ResponseShouldHaveCorrectPriceAndSeats() {
        Flight flight = banaoFlight();
        flight.setPrice(7500.0);
        flight.setAvailableSeats(100);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        FlightResponse res = flightServiceImpl.getFlightById(1L);

        assertEquals(7500.0, res.getPrice());
        assertEquals(100, res.getAvailableSeats());
    }
}
