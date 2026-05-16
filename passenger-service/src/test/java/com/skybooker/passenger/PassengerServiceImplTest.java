package com.skybooker.passenger;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.SeatAssignRequest;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import com.skybooker.passenger.service.PassengerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @InjectMocks
    private PassengerServiceImpl passengerService;

    private PassengerRequest createRequest() {
        PassengerRequest req = new PassengerRequest();
        req.setBookingId("BK-123");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setPassengerType("ADULT");
        req.setDateOfBirth(LocalDate.now().minusYears(30));
        req.setPassportExpiry(LocalDate.now().plusYears(5));
        return req;
    }

    @Test
    void addPassenger_Success() {
        PassengerRequest req = createRequest();
        when(passengerRepository.save(any(PassengerInfo.class))).thenAnswer(i -> i.getArgument(0));

        PassengerResponse res = passengerService.addPassenger(req);

        assertNotNull(res);
        assertEquals("John", res.getFirstName());
        verify(passengerRepository, times(1)).save(any());
    }

    @Test
    void addPassenger_ExpiredPassport_ThrowsException() {
        PassengerRequest req = createRequest();
        req.setPassportExpiry(LocalDate.now().minusDays(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> passengerService.addPassenger(req));
        
        assertTrue(ex.getMessage().contains("Passport is expired"));
    }

    @Test
    void addPassenger_InfantOverAge_ThrowsException() {
        PassengerRequest req = createRequest();
        req.setPassengerType("INFANT");
        req.setDateOfBirth(LocalDate.now().minusYears(3)); // 3 years old

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> passengerService.addPassenger(req));
        
        assertTrue(ex.getMessage().contains("Infant must be under 2 years"));
    }

    @Test
    void addPassenger_ChildOverAge_ThrowsException() {
        PassengerRequest req = createRequest();
        req.setPassengerType("CHILD");
        req.setDateOfBirth(LocalDate.now().minusYears(15)); // 15 years old

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> passengerService.addPassenger(req));
        
        assertTrue(ex.getMessage().contains("Child passenger must be under 12 years"));
    }

    @Test
    void getPassengerById_Success() {
        PassengerInfo p = new PassengerInfo();
        p.setPassengerId(1L);
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(p));

        PassengerResponse res = passengerService.getPassengerById(1L);
        assertEquals(1L, res.getPassengerId());
    }

    @Test
    void getPassengerById_NotFound_ThrowsException() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> passengerService.getPassengerById(1L));
    }

    @Test
    void getPassengersByBooking_ReturnsList() {
        when(passengerRepository.findByBookingId("BK-1")).thenReturn(List.of(new PassengerInfo()));
        List<PassengerResponse> list = passengerService.getPassengersByBooking("BK-1");
        assertEquals(1, list.size());
    }

    @Test
    void getByPassportNumber_Success() {
        PassengerInfo p = new PassengerInfo();
        p.setPassportNumber("A1");
        when(passengerRepository.findByPassportNumber("A1")).thenReturn(Optional.of(p));

        PassengerResponse res = passengerService.getByPassportNumber("A1");
        assertEquals("A1", res.getPassportNumber());
    }

    @Test
    void getByTicketNumber_Success() {
        PassengerInfo p = new PassengerInfo();
        p.setTicketNumber("T1");
        when(passengerRepository.findByTicketNumber("T1")).thenReturn(Optional.of(p));

        PassengerResponse res = passengerService.getByTicketNumber("T1");
        assertEquals("T1", res.getTicketNumber());
    }

    @Test
    void updatePassenger_Success() {
        PassengerInfo p = new PassengerInfo();
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(passengerRepository.save(any())).thenReturn(p);

        PassengerResponse res = passengerService.updatePassenger(1L, createRequest());
        assertEquals("Passenger updated successfully", res.getMessage());
    }

    @Test
    void assignSeat_Success() {
        PassengerInfo p = new PassengerInfo();
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(passengerRepository.findBySeatId(10L)).thenReturn(Optional.empty());
        when(passengerRepository.save(any())).thenReturn(p);

        SeatAssignRequest req = new SeatAssignRequest();
        req.setPassengerId(1L);
        req.setSeatId(10L);
        req.setSeatNumber("12A");

        PassengerResponse res = passengerService.assignSeat(req);
        assertEquals("Seat assigned successfully", res.getMessage());
    }

    @Test
    void assignSeat_SeatAlreadyTaken_ThrowsException() {
        PassengerInfo p = new PassengerInfo();
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(passengerRepository.findBySeatId(10L)).thenReturn(Optional.of(new PassengerInfo()));

        SeatAssignRequest req = new SeatAssignRequest();
        req.setPassengerId(1L);
        req.setSeatId(10L);

        assertThrows(IllegalStateException.class, () -> passengerService.assignSeat(req));
    }

    @Test
    void deletePassenger_Success() {
        when(passengerRepository.existsById(1L)).thenReturn(true);
        passengerService.deletePassenger(1L);
        verify(passengerRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteByBookingId_Success() {
        passengerService.deleteByBookingId("BK-1");
        verify(passengerRepository, times(1)).deleteByBookingId("BK-1");
    }

    @Test
    void getPassengerCount_ReturnsCount() {
        when(passengerRepository.countByBookingId("BK-1")).thenReturn(5);
        assertEquals(5, passengerService.getPassengerCount("BK-1"));
    }
}
