package com.skybooker.seat.service;

import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Override
    public SeatResponse addSeat(SeatRequest request) {

        // same seat already exist toh nahi karta check karo
        boolean exists = seatRepository
                .findByFlightIdAndSeatNumber(request.getFlightId(), request.getSeatNumber())
                .isPresent();

        if (exists) {
            throw new RuntimeException("Seat " + request.getSeatNumber()
                    + " already exists for flight: " + request.getFlightId());
        }

        Seat seat = new Seat();
        seat.setFlightId(request.getFlightId());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatClass(request.getSeatClass());
        seat.setRow(request.getRow());
        seat.setColumn(request.getColumn());
        seat.setWindow(request.isWindow());
        seat.setAisle(request.isAisle());
        seat.setHasExtraLegroom(request.isHasExtraLegroom());
        seat.setPriceMultiplier(request.getPriceMultiplier());
        seat.setStatus("AVAILABLE");

        Seat saved = seatRepository.save(seat);
        return mapToResponse(saved, "Seat added successfully");
    }

    @Override
    public List<SeatResponse> getSeatsByFlight(Long flightId) {
        return seatRepository.findByFlightId(flightId)
                .stream()
                .map(s -> mapToResponse(s, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatResponse> getAvailableSeats(Long flightId) {
        return seatRepository.findByFlightIdAndStatus(flightId, "AVAILABLE")
                .stream()
                .map(s -> mapToResponse(s, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatResponse> getSeatsByClass(Long flightId, String seatClass) {
        return seatRepository.findByFlightIdAndSeatClass(flightId, seatClass)
                .stream()
                .map(s -> mapToResponse(s, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public SeatResponse holdSeat(Long flightId, String seatNumber) {

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));

        // sirf AVAILABLE seat hold ho sakti hai
        if (!seat.getStatus().equals("AVAILABLE")) {
            throw new RuntimeException("Seat " + seatNumber + " is not available. Current status: " + seat.getStatus());
        }

        // 15 minute ki hold lagao
        seat.setStatus("HELD");
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));

        Seat updated = seatRepository.save(seat);
        return mapToResponse(updated, "Seat held for 15 minutes. Please complete payment.");
    }

    @Override
    public SeatResponse confirmSeat(Long flightId, String seatNumber) {

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));

        // sirf HELD seat confirm ho sakti hai
        if (!seat.getStatus().equals("HELD")) {
            throw new RuntimeException("Seat " + seatNumber + " is not held. Cannot confirm.");
        }

        // hold expire toh nahi ho gayi check karo
        if (seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus("AVAILABLE");
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new RuntimeException("Seat hold expired. Please select the seat again.");
        }

        seat.setStatus("CONFIRMED");
        seat.setHoldExpiresAt(null);

        Seat updated = seatRepository.save(seat);
        return mapToResponse(updated, "Seat confirmed successfully");
    }

    @Override
    public SeatResponse releaseSeat(Long flightId, String seatNumber) {

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));

        // CONFIRMED seat release nahi hogi - sirf HELD wali
        if (seat.getStatus().equals("CONFIRMED")) {
            throw new RuntimeException("Confirmed seat cannot be released directly. Please cancel booking.");
        }

        seat.setStatus("AVAILABLE");
        seat.setHoldExpiresAt(null);

        Seat updated = seatRepository.save(seat);
        return mapToResponse(updated, "Seat released successfully");
    }

    @Override
    public int getAvailableCount(Long flightId) {
        return seatRepository.countByFlightIdAndStatus(flightId, "AVAILABLE");
    }

    // har 2 minute mein run hoga - expired holds release karne ke liye
    @Scheduled(fixedDelay = 120000)
    @Override
    public void releaseExpiredHolds() {
        List<Seat> expiredSeats = seatRepository
                .findByStatusAndHoldExpiresAtBefore("HELD", LocalDateTime.now());

        for (Seat seat : expiredSeats) {
            seat.setStatus("AVAILABLE");
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            System.out.println("Released expired hold for seat: " + seat.getSeatNumber()
                    + " on flight: " + seat.getFlightId());
        }
    }

    private SeatResponse mapToResponse(Seat seat, String message) {
        SeatResponse res = new SeatResponse();
        res.setId(seat.getId());
        res.setFlightId(seat.getFlightId());
        res.setSeatNumber(seat.getSeatNumber());
        res.setSeatClass(seat.getSeatClass());
        res.setRow(seat.getRow());
        res.setColumn(seat.getColumn());
        res.setWindow(seat.isWindow());
        res.setAisle(seat.isAisle());
        res.setHasExtraLegroom(seat.isHasExtraLegroom());
        res.setStatus(seat.getStatus());
        res.setPriceMultiplier(seat.getPriceMultiplier());
        res.setHoldExpiresAt(seat.getHoldExpiresAt());
        res.setMessage(message);
        return res;
    }
}
