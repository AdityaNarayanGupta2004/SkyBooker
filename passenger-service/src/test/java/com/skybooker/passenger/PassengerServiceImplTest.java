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
import java.time.LocalDateTime;
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
    private PassengerServiceImpl passengerServiceImpl;

    // Helper — ready-made PassengerInfo banao
    private PassengerInfo banaoPassenger() {
        PassengerInfo p = new PassengerInfo();
        p.setPassengerId(1L);
        p.setBookingId("BK-001");
        p.setTitle("Mr");
        p.setFirstName("Rahul");
        p.setLastName("Sharma");
        p.setDateOfBirth(LocalDate.of(1995, 5, 10));
        p.setGender("MALE");
        p.setPassportNumber("A1234567");
        p.setNationality("Indian");
        p.setPassportExpiry(LocalDate.now().plusYears(3)); // valid passport
        p.setPassengerType("ADULT");
        p.setTicketNumber("TKT-ABCD1234");
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    // Helper — ready-made PassengerRequest banao
    private PassengerRequest banaoRequest(String type) {
        PassengerRequest req = new PassengerRequest();
        req.setBookingId("BK-001");
        req.setTitle("Mr");
        req.setFirstName("Rahul");
        req.setLastName("Sharma");
        req.setDateOfBirth(LocalDate.of(1995, 5, 10));
        req.setGender("MALE");
        req.setPassportNumber("A1234567");
        req.setNationality("Indian");
        req.setPassportExpiry(LocalDate.now().plusYears(3));
        req.setPassengerType(type);
        return req;
    }

    // ---------------------------------------------------------------
    // ADD PASSENGER TESTS
    // ---------------------------------------------------------------

    // Test 1: Adult passenger successfully add ho
    @Test
    void addPassenger_ValidAdult_ShouldSucceed() {
        PassengerRequest req = banaoRequest("ADULT");
        PassengerInfo saved = banaoPassenger();

        when(passengerRepository.save(any(PassengerInfo.class))).thenReturn(saved);

        PassengerResponse res = passengerServiceImpl.addPassenger(req);

        assertNotNull(res);
        assertEquals("Rahul", res.getFirstName());
        assertEquals("ADULT", res.getPassengerType());
        assertNotNull(res.getTicketNumber()); // ticket generate hona chahiye
    }

    // Test 2: Expired passport wala passenger add nahi ho sakta
    @Test
    void addPassenger_WithExpiredPassport_ShouldThrowException() {
        PassengerRequest req = banaoRequest("ADULT");
        req.setPassportExpiry(LocalDate.now().minusDays(10)); // expired

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.addPassenger(req));

        assertTrue(ex.getMessage().contains("Passport is expired"));
        verify(passengerRepository, never()).save(any());
    }

    // Test 3: Infant jo 2 saal se zyada ka hai add nahi ho sakta
    @Test
    void addPassenger_InfantOlderThan2Years_ShouldThrowException() {
        PassengerRequest req = banaoRequest("INFANT");
        req.setDateOfBirth(LocalDate.now().minusYears(3)); // 3 saal ka — infant nahi

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.addPassenger(req));

        assertTrue(ex.getMessage().contains("Infant must be under 2 years"));
        verify(passengerRepository, never()).save(any());
    }

    // Test 4: Child jo 12 saal se zyada ka hai add nahi ho sakta
    @Test
    void addPassenger_ChildOlderThan12Years_ShouldThrowException() {
        PassengerRequest req = banaoRequest("CHILD");
        req.setDateOfBirth(LocalDate.now().minusYears(13)); // 13 saal ka — child nahi

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.addPassenger(req));

        assertTrue(ex.getMessage().contains("Child passenger must be under 12 years"));
        verify(passengerRepository, never()).save(any());
    }

    // Test 5: Valid infant (1 saal ka) add ho jaye
    @Test
    void addPassenger_ValidInfant_ShouldSucceed() {
        PassengerRequest req = banaoRequest("INFANT");
        req.setDateOfBirth(LocalDate.now().minusMonths(8)); // 8 mahine ka infant — valid

        PassengerInfo saved = banaoPassenger();
        saved.setPassengerType("INFANT");

        when(passengerRepository.save(any(PassengerInfo.class))).thenReturn(saved);

        PassengerResponse res = passengerServiceImpl.addPassenger(req);

        assertEquals("INFANT", res.getPassengerType());
    }

    // ---------------------------------------------------------------
    // GET PASSENGER TESTS
    // ---------------------------------------------------------------

    // Test 6: ID se passenger mile
    @Test
    void getPassengerById_WhenExists_ShouldReturnPassenger() {
        PassengerInfo passenger = banaoPassenger();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        PassengerResponse res = passengerServiceImpl.getPassengerById(1L);

        assertNotNull(res);
        assertEquals("Rahul", res.getFirstName());
        assertEquals("BK-001", res.getBookingId());
    }

    // Test 7: ID na ho toh exception aaye
    @Test
    void getPassengerById_WhenNotFound_ShouldThrowException() {
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.getPassengerById(99L));

        assertTrue(ex.getMessage().contains("Passenger not found with id"));
    }

    // Test 8: Booking ke saare passengers milein
    @Test
    void getPassengersByBooking_ShouldReturnAllPassengers() {
        PassengerInfo p1 = banaoPassenger();
        PassengerInfo p2 = banaoPassenger();
        p2.setPassengerId(2L);
        p2.setFirstName("Priya");

        when(passengerRepository.findByBookingId("BK-001")).thenReturn(List.of(p1, p2));

        List<PassengerResponse> result = passengerServiceImpl.getPassengersByBooking("BK-001");

        assertEquals(2, result.size());
    }

    // Test 9: Passport number se passenger mile
    @Test
    void getByPassportNumber_WhenExists_ShouldReturnPassenger() {
        PassengerInfo passenger = banaoPassenger();

        when(passengerRepository.findByPassportNumber("A1234567"))
                .thenReturn(Optional.of(passenger));

        PassengerResponse res = passengerServiceImpl.getByPassportNumber("A1234567");

        assertEquals("A1234567", res.getPassportNumber());
    }

    // Test 10: Galat passport number pe exception aaye
    @Test
    void getByPassportNumber_WhenNotFound_ShouldThrowException() {
        when(passengerRepository.findByPassportNumber("Z9999999"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.getByPassportNumber("Z9999999"));

        assertTrue(ex.getMessage().contains("Passenger not found with passport"));
    }

    // ---------------------------------------------------------------
    // SEAT ASSIGN TESTS
    // ---------------------------------------------------------------

    // Test 11: Seat successfully assign ho
    @Test
    void assignSeat_WhenSeatIsFree_ShouldSucceed() {
        PassengerInfo passenger = banaoPassenger();
        SeatAssignRequest req = new SeatAssignRequest(1L, 50L, "12A");

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.findBySeatId(50L)).thenReturn(Optional.empty()); // seat free hai
        when(passengerRepository.save(any(PassengerInfo.class))).thenAnswer(i -> i.getArgument(0));

        PassengerResponse res = passengerServiceImpl.assignSeat(req);

        assertEquals("12A", res.getSeatNumber());
    }

    // Test 12: Pehle se liya hua seat assign nahi ho sakta
    @Test
    void assignSeat_WhenSeatAlreadyTaken_ShouldThrowException() {
        PassengerInfo passenger = banaoPassenger();
        PassengerInfo anotherPassenger = banaoPassenger();
        anotherPassenger.setPassengerId(2L);

        SeatAssignRequest req = new SeatAssignRequest(1L, 50L, "12A");

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.findBySeatId(50L)).thenReturn(Optional.of(anotherPassenger));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.assignSeat(req));

        assertTrue(ex.getMessage().contains("already assigned"));
    }

    // ---------------------------------------------------------------
    // DELETE & COUNT TESTS
    // ---------------------------------------------------------------

    // Test 13: Passenger delete ho jaye
    @Test
    void deletePassenger_WhenExists_ShouldDeleteSuccessfully() {
        when(passengerRepository.existsById(1L)).thenReturn(true);

        passengerServiceImpl.deletePassenger(1L);

        verify(passengerRepository, times(1)).deleteById(1L);
    }

    // Test 14: Passenger na ho toh delete pe exception aaye
    @Test
    void deletePassenger_WhenNotFound_ShouldThrowException() {
        when(passengerRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passengerServiceImpl.deletePassenger(99L));

        assertTrue(ex.getMessage().contains("Passenger not found with id"));
        verify(passengerRepository, never()).deleteById(any());
    }

    // Test 15: Booking ka passenger count sahi aaye
    @Test
    void getPassengerCount_ShouldReturnCorrectCount() {
        when(passengerRepository.countByBookingId("BK-001")).thenReturn(3);

        int count = passengerServiceImpl.getPassengerCount("BK-001");

        assertEquals(3, count);
    }
}
