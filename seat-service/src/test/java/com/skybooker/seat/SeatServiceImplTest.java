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
    private SeatServiceImpl seatService;

    @Test
    void addSeat_Success() {
        SeatRequest req = new SeatRequest();
        req.setFlightId(1L);
        req.setSeatNumber("1A");
        
        when(seatRepository.findByFlightIdAndSeatNumber(1L, "1A")).thenReturn(Optional.empty());
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatResponse res = seatService.addSeat(req);
        assertEquals("1A", res.getSeatNumber());
    }

    @Test
    void holdSeat_Success() {
        Seat s = new Seat();
        s.setStatus("AVAILABLE");
        when(seatRepository.findByFlightIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(s));
        when(seatRepository.save(any())).thenReturn(s);

        SeatResponse res = seatService.holdSeat(1L, "1A");
        assertEquals("HELD", res.getStatus());
    }

    @Test
    void confirmSeat_Success() {
        Seat s = new Seat();
        s.setStatus("HELD");
        s.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(seatRepository.findByFlightIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(s));
        when(seatRepository.save(any())).thenReturn(s);

        SeatResponse res = seatService.confirmSeat(1L, "1A");
        assertEquals("CONFIRMED", res.getStatus());
    }

    @Test
    void confirmSeat_Expired_ThrowsException() {
        Seat s = new Seat();
        s.setStatus("HELD");
        s.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(seatRepository.findByFlightIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(s));

        assertThrows(IllegalStateException.class, () -> seatService.confirmSeat(1L, "1A"));
    }

    @Test
    void releaseSeat_Success() {
        Seat s = new Seat();
        s.setStatus("HELD");
        when(seatRepository.findByFlightIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(s));
        when(seatRepository.save(any())).thenReturn(s);

        SeatResponse res = seatService.releaseSeat(1L, "1A");
        assertEquals("AVAILABLE", res.getStatus());
    }

    @Test
    void getAvailableCount_ReturnsCount() {
        when(seatRepository.countByFlightIdAndStatus(1L, "AVAILABLE")).thenReturn(150);
        assertEquals(150, seatService.getAvailableCount(1L));
    }

    @Test
    void releaseExpiredHolds_Success() {
        Seat s = new Seat();
        when(seatRepository.findByStatusAndHoldExpiresAtBefore(eq("HELD"), any())).thenReturn(List.of(s));
        
        seatService.releaseExpiredHolds();
        verify(seatRepository, times(1)).save(s);
        assertEquals("AVAILABLE", s.getStatus());
    }
}
