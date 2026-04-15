package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.SeatAssignRequest;

import java.util.List;

public interface PassengerService {

    PassengerResponse addPassenger(PassengerRequest request);

    PassengerResponse getPassengerById(Long passengerId);

    List<PassengerResponse> getPassengersByBooking(String bookingId);

    PassengerResponse getByPassportNumber(String passportNumber);

    PassengerResponse getByTicketNumber(String ticketNumber);

    PassengerResponse updatePassenger(Long passengerId, PassengerRequest request);

    PassengerResponse assignSeat(SeatAssignRequest request);

    void deletePassenger(Long passengerId);

    void deleteByBookingId(String bookingId);

    int getPassengerCount(String bookingId);
}
