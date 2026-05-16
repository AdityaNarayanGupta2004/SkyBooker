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
    private AirlineServiceImpl airlineService;

    // ==================== AIRLINE TESTS ====================

    @Test
    void addAirline_Success() {
        AirlineRequest req = new AirlineRequest();
        req.setIataCode("AI");
        req.setName("Air India");
        
        when(airlineRepository.existsByIataCode("AI")).thenReturn(false);
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> {
            Airline a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        AirlineResponse res = airlineService.addAirline(req);
        assertEquals(1L, res.getId());
        assertEquals("AI", res.getIataCode());
    }

    @Test
    void addAirline_DuplicateIata_ThrowsException() {
        AirlineRequest req = new AirlineRequest();
        req.setIataCode("AI");
        when(airlineRepository.existsByIataCode("AI")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> airlineService.addAirline(req));
    }

    @Test
    void getAirlineById_Success() {
        Airline a = new Airline();
        a.setId(1L);
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(a));
        assertEquals(1L, airlineService.getAirlineById(1L).getId());
    }

    @Test
    void getAirlineByIata_Success() {
        Airline a = new Airline();
        a.setIataCode("AI");
        when(airlineRepository.findByIataCode("AI")).thenReturn(Optional.of(a));
        assertEquals("AI", airlineService.getAirlineByIata("AI").getIataCode());
    }

    @Test
    void getAllAirlines_ReturnsList() {
        when(airlineRepository.findAll()).thenReturn(List.of(new Airline()));
        assertEquals(1, airlineService.getAllAirlines().size());
    }

    @Test
    void getActiveAirlines_ReturnsList() {
        when(airlineRepository.findByIsActive(true)).thenReturn(List.of(new Airline()));
        assertEquals(1, airlineService.getActiveAirlines().size());
    }

    @Test
    void updateAirline_Success() {
        Airline a = new Airline();
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(a));
        when(airlineRepository.save(any())).thenReturn(a);
        assertNotNull(airlineService.updateAirline(1L, new AirlineRequest()));
    }

    @Test
    void toggleAirlineStatus_Success() {
        Airline a = new Airline();
        a.setActive(true);
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(a));
        when(airlineRepository.save(any())).thenReturn(a);
        
        AirlineResponse res = airlineService.toggleAirlineStatus(1L);
        assertFalse(a.isActive()); // toggled from true to false
    }

    // ==================== AIRPORT TESTS ====================

    @Test
    void addAirport_Success() {
        AirportRequest req = new AirportRequest();
        req.setIataCode("DEL");
        req.setName("IGI");
        
        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any(Airport.class))).thenAnswer(i -> {
            Airport a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AirportResponse res = airlineService.addAirport(req);
        assertEquals(10L, res.getId());
    }

    @Test
    void addAirport_DuplicateIata_ThrowsException() {
        AirportRequest req = new AirportRequest();
        req.setIataCode("DEL");
        when(airportRepository.existsByIataCode("DEL")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> airlineService.addAirport(req));
    }

    @Test
    void getAirportById_Success() {
        Airport a = new Airport();
        a.setId(10L);
        when(airportRepository.findById(10L)).thenReturn(Optional.of(a));
        assertEquals(10L, airlineService.getAirportById(10L).getId());
    }

    @Test
    void getAirportByIata_Success() {
        Airport a = new Airport();
        a.setIataCode("DEL");
        when(airportRepository.findByIataCode("DEL")).thenReturn(Optional.of(a));
        assertEquals("DEL", airlineService.getAirportByIata("DEL").getIataCode());
    }

    @Test
    void searchAirports_ReturnsList() {
        when(airportRepository.findByCityContainingIgnoreCaseOrNameContainingIgnoreCase("DEL", "DEL"))
                .thenReturn(List.of(new Airport()));
        assertEquals(1, airlineService.searchAirports("DEL").size());
    }

    @Test
    void updateAirport_Success() {
        Airport a = new Airport();
        when(airportRepository.findById(10L)).thenReturn(Optional.of(a));
        when(airportRepository.save(any())).thenReturn(a);
        assertNotNull(airlineService.updateAirport(10L, new AirportRequest()));
    }
}