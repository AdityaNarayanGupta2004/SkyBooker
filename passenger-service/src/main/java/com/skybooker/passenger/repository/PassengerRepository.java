package com.skybooker.passenger.repository;

import com.skybooker.passenger.entity.PassengerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassengerRepository extends JpaRepository<PassengerInfo, Long> {

    // ek booking ke saare passengers
    List<PassengerInfo> findByBookingId(String bookingId);

    // passport number se dhundho
    Optional<PassengerInfo> findByPassportNumber(String passportNumber);

    // ticket number se dhundho
    Optional<PassengerInfo> findByTicketNumber(String ticketNumber);

    // seat assign check ke liye
    Optional<PassengerInfo> findBySeatId(Long seatId);

    // ek booking mein kitne passengers hain
    int countByBookingId(String bookingId);

    // booking cancel hone pe saare passengers delete karo
    void deleteByBookingId(String bookingId);
}
