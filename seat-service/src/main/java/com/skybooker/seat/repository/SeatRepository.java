package com.skybooker.seat.repository;

import com.skybooker.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // flight ke saare seats
    List<Seat> findByFlightId(Long flightId);

    // flight ke available seats
    List<Seat> findByFlightIdAndStatus(Long flightId, String status);

    // class ke hisaab se seats - ECONOMY, BUSINESS, FIRST
    List<Seat> findByFlightIdAndSeatClass(Long flightId, String seatClass);

    // seat number se dhundho - jaise 12A
    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

    // expired holds release karne ke liye - scheduler use karega
    List<Seat> findByStatusAndHoldExpiresAtBefore(String status, LocalDateTime time);

    // flight ke available seats count
    int countByFlightIdAndStatus(Long flightId, String status);
}
