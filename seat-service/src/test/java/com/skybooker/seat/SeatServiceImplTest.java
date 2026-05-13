package com.skybooker.seat;

import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.repository.SeatRepository;
import com.skybooker.seat.service.SeatServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatServiceImpl seatServiceImpl;

    // Helper — ek ready-made Seat banao
    private Seat banaoSeat(String status) {
        Seat s = new Seat();
        s.setId(1L);
        s.setFlightId(101L);
        s.setSeatNumber("12A");
        s.setSeatClass("ECONOMY");
        s.setRow(12);
        s.setColumn("A");
        s.setWindow(true);
        s.setAisle(false);
        s.setHasExtraLegroom(false);
        s.setPriceMultiplier(1.0);
        s.setStatus(status);
        s.setHoldExpiresAt(null);
        return s;
    }

    // Helper — SeatRequest banao
    private SeatRequest banaoRequest() {
        SeatRequest req = new SeatRequest();
        req.setFlightId(101L);
        req.setSeatNumber("12A");
        req.setSeatClass("ECONOMY");
        req.setRow(12);
        req.setColumn("A");
        req.setWindow(true);
        req.setAisle(false);
        req.setHasExtraLegroom(false);
        req.setPriceMultiplier(1.0);
        return req;
    }

    // Test 1: Naya seat add ho jaye
    @Test
    void addSeat_WhenSeatDoesNotExist_ShouldSucceed() {
        SeatRequest req = banaoRequest();
        Seat saved = banaoSeat("AVAILABLE");

        when(seatRepository.findByFlightIdAndSeatNumber(req.getFlightId(), req.getSeatNumber()))
                .thenReturn(Optional.empty());
        when(seatRepository.save(any(Seat.class))).thenReturn(saved);

        SeatResponse res = seatServiceImpl.addSeat(req);

        assertNotNull(res);
        assertEquals("12A", res.getSeatNumber());
        assertEquals("AVAILABLE", res.getStatus());
    }

    // Test 2: Same seat dobara add karne pe exception aaye
    @Test
    void addSeat_WhenSeatAlreadyExists_ShouldThrowException() {
        SeatRequest req = banaoRequest();
        Seat existing = banaoSeat("AVAILABLE");

        when(seatRepository.findByFlightIdAndSeatNumber(req.getFlightId(), req.getSeatNumber()))
                .thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> seatServiceImpl.addSeat(req));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(seatRepository, never()).save(any());
    }

    // Test 3: Flight ke saare seats milein
    @Test
    void getSeatsByFlight_ShouldReturnAllSeats() {
        Seat s1 = banaoSeat("AVAILABLE");
        Seat s2 = banaoSeat("HELD");
        s2.setSeatNumber("12B");

        when(seatRepository.findByFlightId(101L)).thenReturn(List.of(s1, s2));

        List<SeatResponse> result = seatServiceImpl.getSeatsByFlight(101L);

        assertEquals(2, result.size());
    }

    // Test 4: Class ke hisaab se seats milein
    @Test
    void getSeatsByClass_ShouldReturnOnlyThatClass() {
        Seat s = banaoSeat("AVAILABLE");
        s.setSeatClass("BUSINESS");

        when(seatRepository.findByFlightIdAndSeatClass(101L, "BUSINESS"))
                .thenReturn(List.of(s));

        List<SeatResponse> result = seatServiceImpl.getSeatsByClass(101L, "BUSINESS");

        assertEquals(1, result.size());
        assertEquals("BUSINESS", result.get(0).getSeatClass());
    }

    // Test 5: Available seat hold ho jaye
    @Test
    void holdSeat_WhenSeatIsAvailable_ShouldChangeStatusToHeld() {
        Seat seat = banaoSeat("AVAILABLE");

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(i -> i.getArgument(0));

        SeatResponse res = seatServiceImpl.holdSeat(101L, "12A");

        assertEquals("HELD", res.getStatus());
        assertNotNull(res.getHoldExpiresAt()); // 15 min expiry set honi chahiye
    }

    // Test 6: Pehle se held seat ko dobara hold nahi kar sakte
    @Test
    void holdSeat_WhenSeatAlreadyHeld_ShouldThrowException() {
        Seat seat = banaoSeat("HELD");

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> seatServiceImpl.holdSeat(101L, "12A"));

        assertTrue(ex.getMessage().contains("not available"));
    }

    // Test 7: Held seat confirm ho jaye
    @Test
    void confirmSeat_WhenSeatIsHeld_ShouldChangeStatusToConfirmed() {
        Seat seat = banaoSeat("HELD");
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10)); // abhi expire nahi hui

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(i -> i.getArgument(0));

        SeatResponse res = seatServiceImpl.confirmSeat(101L, "12A");

        assertEquals("CONFIRMED", res.getStatus());
    }

    // Test 8: Hold expire ho gayi ho toh confirm pe exception aaye
    @Test
    void confirmSeat_WhenHoldExpired_ShouldThrowException() {
        Seat seat = banaoSeat("HELD");
        // Hold 5 minute pehle expire ho gayi
        seat.setHoldExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(i -> i.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> seatServiceImpl.confirmSeat(101L, "12A"));

        assertTrue(ex.getMessage().contains("hold expired"));
    }

    // Test 9: Held seat release ho jaye
    @Test
    void releaseSeat_WhenSeatIsHeld_ShouldMakeItAvailable() {
        Seat seat = banaoSeat("HELD");

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(i -> i.getArgument(0));

        SeatResponse res = seatServiceImpl.releaseSeat(101L, "12A");

        assertEquals("AVAILABLE", res.getStatus());
    }

    // Test 10: Confirmed seat directly release nahi ho sakti
    @Test
    void releaseSeat_WhenSeatIsConfirmed_ShouldThrowException() {
        Seat seat = banaoSeat("CONFIRMED");

        when(seatRepository.findByFlightIdAndSeatNumber(101L, "12A"))
                .thenReturn(Optional.of(seat));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> seatServiceImpl.releaseSeat(101L, "12A"));

        assertTrue(ex.getMessage().contains("Confirmed seat"));
    }
}
