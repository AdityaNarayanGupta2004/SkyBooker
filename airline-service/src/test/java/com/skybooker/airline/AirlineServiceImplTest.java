package com.skybooker.airline;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.repository.AirportRepository;
import com.skybooker.airline.service.AirlineServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceImplTest {

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private AirportRepository airportRepository;

    @InjectMocks
    private AirlineServiceImpl airlineServiceImpl;

    // Helper — ready-made Airline banao
    private Airline banaoAirline() {
        Airline a = new Airline();
        a.setId(1L);
        a.setName("IndiGo");
        a.setIataCode("6E");
        a.setIcaoCode("IGO");
        a.setCountry("India");
        a.setContactEmail("support@indigo.com");
        a.setContactPhone("9999999999");
        a.setActive(true);
        return a;
    }

    // Helper — ready-made Airport banao
    private Airport banaoAirport() {
        Airport a = new Airport();
        a.setId(1L);
        a.setName("Indira Gandhi International Airport");
        a.setIataCode("DEL");
        a.setIcaoCode("VIDP");
        a.setCity("Delhi");
        a.setCountry("India");
        a.setLatitude(28.5665);
        a.setLongitude(77.1031);
        a.setTimezone("Asia/Kolkata");
        return a;
    }

    // Helper — ready-made AirlineRequest banao
    private AirlineRequest banaoAirlineRequest() {
        AirlineRequest req = new AirlineRequest();
        req.setName("IndiGo");
        req.setIataCode("6E");
        req.setIcaoCode("IGO");
        req.setCountry("India");
        req.setContactEmail("support@indigo.com");
        req.setContactPhone("9999999999");
        return req;
    }

    // Helper — ready-made AirportRequest banao
    private AirportRequest banaoAirportRequest() {
        AirportRequest req = new AirportRequest();
        req.setName("Indira Gandhi International Airport");
        req.setIataCode("DEL");
        req.setIcaoCode("VIDP");
        req.setCity("Delhi");
        req.setCountry("India");
        req.setLatitude(28.5665);
        req.setLongitude(77.1031);
        req.setTimezone("Asia/Kolkata");
        return req;
    }

    // ---------------------------------------------------------------
    // AIRLINE TESTS
    // ---------------------------------------------------------------

    // Test 1: Nai airline add ho jaye
    @Test
    void addAirline_WhenIataCodeIsUnique_ShouldSucceed() {
        AirlineRequest req = banaoAirlineRequest();
        Airline saved = banaoAirline();

        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any(Airline.class))).thenReturn(saved);

        AirlineResponse res = airlineServiceImpl.addAirline(req);

        assertNotNull(res);
        assertEquals("IndiGo", res.getName());
        assertEquals("6E", res.getIataCode());
        assertTrue(res.isActive());
    }

    // Test 2: Same IATA code se dobara airline add nahi ho sakti
    @Test
    void addAirline_WhenIataCodeAlreadyExists_ShouldThrowException() {
        AirlineRequest req = banaoAirlineRequest();

        when(airlineRepository.existsByIataCode("6E")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> airlineServiceImpl.addAirline(req));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(airlineRepository, never()).save(any());
    }

    // Test 3: ID se airline mile
    @Test
    void getAirlineById_WhenExists_ShouldReturnAirline() {
        Airline airline = banaoAirline();

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));

        AirlineResponse res = airlineServiceImpl.getAirlineById(1L);

        assertNotNull(res);
        assertEquals("IndiGo", res.getName());
        assertEquals("6E", res.getIataCode());
    }

    // Test 4: Galat ID se exception aaye
    @Test
    void getAirlineById_WhenNotFound_ShouldThrowException() {
        when(airlineRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> airlineServiceImpl.getAirlineById(99L));

        assertTrue(ex.getMessage().contains("Airline not found with id"));
    }

    // Test 5: IATA code se airline mile
    @Test
    void getAirlineByIata_WhenExists_ShouldReturnAirline() {
        Airline airline = banaoAirline();

        when(airlineRepository.findByIataCode("6E")).thenReturn(Optional.of(airline));

        AirlineResponse res = airlineServiceImpl.getAirlineByIata("6E");

        assertEquals("IndiGo", res.getName());
    }

    // Test 6: Galat IATA code se exception aaye
    @Test
    void getAirlineByIata_WhenNotFound_ShouldThrowException() {
        when(airlineRepository.findByIataCode("XX")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> airlineServiceImpl.getAirlineByIata("XX"));

        assertTrue(ex.getMessage().contains("Airline not found with IATA code"));
    }

    // Test 7: Saari airlines milein
    @Test
    void getAllAirlines_ShouldReturnAll() {
        Airline a1 = banaoAirline();
        Airline a2 = banaoAirline();
        a2.setId(2L);
        a2.setName("Air India");
        a2.setIataCode("AI");

        when(airlineRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AirlineResponse> result = airlineServiceImpl.getAllAirlines();

        assertEquals(2, result.size());
    }

    // Test 8: Sirf active airlines milein
    @Test
    void getActiveAirlines_ShouldReturnOnlyActiveOnes() {
        Airline active = banaoAirline();

        when(airlineRepository.findByIsActive(true)).thenReturn(List.of(active));

        List<AirlineResponse> result = airlineServiceImpl.getActiveAirlines();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // Test 9: Airline update ho jaye
    @Test
    void updateAirline_WhenExists_ShouldUpdateSuccessfully() {
        Airline existing = banaoAirline();
        AirlineRequest req = banaoAirlineRequest();
        req.setName("IndiGo Airlines");
        req.setContactEmail("new@indigo.com");

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> i.getArgument(0));

        AirlineResponse res = airlineServiceImpl.updateAirline(1L, req);

        assertEquals("IndiGo Airlines", res.getName());
        assertEquals("new@indigo.com", res.getContactEmail());
    }

    // Test 10: Active airline deactivate ho jaye
    @Test
    void toggleAirlineStatus_WhenActive_ShouldDeactivate() {
        Airline airline = banaoAirline();
        airline.setActive(true);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> i.getArgument(0));

        AirlineResponse res = airlineServiceImpl.toggleAirlineStatus(1L);

        assertFalse(res.isActive());
        assertTrue(res.getMessage().contains("deactivated"));
    }

    // Test 11: Inactive airline activate ho jaye
    @Test
    void toggleAirlineStatus_WhenInactive_ShouldActivate() {
        Airline airline = banaoAirline();
        airline.setActive(false);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> i.getArgument(0));

        AirlineResponse res = airlineServiceImpl.toggleAirlineStatus(1L);

        assertTrue(res.isActive());
        assertTrue(res.getMessage().contains("activated"));
    }

    // ---------------------------------------------------------------
    // AIRPORT TESTS
    // ---------------------------------------------------------------

    // Test 12: Naya airport add ho jaye
    @Test
    void addAirport_WhenIataCodeIsUnique_ShouldSucceed() {
        AirportRequest req = banaoAirportRequest();
        Airport saved = banaoAirport();

        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any(Airport.class))).thenReturn(saved);

        AirportResponse res = airlineServiceImpl.addAirport(req);

        assertNotNull(res);
        assertEquals("DEL", res.getIataCode());
        assertEquals("Delhi", res.getCity());
    }

    // Test 13: Same IATA code se airport dobara add nahi ho sakta
    @Test
    void addAirport_WhenIataCodeExists_ShouldThrowException() {
        AirportRequest req = banaoAirportRequest();

        when(airportRepository.existsByIataCode("DEL")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> airlineServiceImpl.addAirport(req));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(airportRepository, never()).save(any());
    }

    // Test 14: ID se airport mile
    @Test
    void getAirportById_WhenExists_ShouldReturnAirport() {
        Airport airport = banaoAirport();

        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport));

        AirportResponse res = airlineServiceImpl.getAirportById(1L);

        assertEquals("DEL", res.getIataCode());
        assertEquals("Delhi", res.getCity());
    }

    // Test 15: Keyword se airports search ho
    @Test
    void searchAirports_ShouldReturnMatchingAirports() {
        Airport airport = banaoAirport();

        when(airportRepository.findByCityContainingIgnoreCaseOrNameContainingIgnoreCase("Delhi", "Delhi"))
                .thenReturn(List.of(airport));

        List<AirportResponse> result = airlineServiceImpl.searchAirports("Delhi");

        assertEquals(1, result.size());
        assertEquals("DEL", result.get(0).getIataCode());
    }

    // Test 16: Koi airport nahi mila toh empty list aaye
    @Test
    void searchAirports_WhenNoMatch_ShouldReturnEmptyList() {
        when(airportRepository.findByCityContainingIgnoreCaseOrNameContainingIgnoreCase("XYZ", "XYZ"))
                .thenReturn(List.of());

        List<AirportResponse> result = airlineServiceImpl.searchAirports("XYZ");

        assertTrue(result.isEmpty());
    }
}