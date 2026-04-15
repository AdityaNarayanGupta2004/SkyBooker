package com.skybooker.seat.service;

import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;

import java.util.List;

public interface SeatService {

    SeatResponse addSeat(SeatRequest request);

    List<SeatResponse> getSeatsByFlight(Long flightId);

    List<SeatResponse> getAvailableSeats(Long flightId);

    List<SeatResponse> getSeatsByClass(Long flightId, String seatClass);

    SeatResponse holdSeat(Long flightId, String seatNumber);

    SeatResponse confirmSeat(Long flightId, String seatNumber);

    SeatResponse releaseSeat(Long flightId, String seatNumber);

    int getAvailableCount(Long flightId);

    void releaseExpiredHolds();
}
