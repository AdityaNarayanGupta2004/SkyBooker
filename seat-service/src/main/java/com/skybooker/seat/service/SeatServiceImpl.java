//package com.skybooker.seat.service;
//
//import com.skybooker.seat.dto.SeatRequest;
//import com.skybooker.seat.dto.SeatResponse;
//import com.skybooker.seat.entity.Seat;
//import com.skybooker.seat.repository.SeatRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class SeatServiceImpl implements SeatService {
//
//    private final SeatRepository seatRepository;
//
//    @Override
//    public SeatResponse addSeat(SeatRequest request) {
//        boolean exists = seatRepository
//                .findByFlightIdAndSeatNumber(request.getFlightId(), request.getSeatNumber())
//                .isPresent();
//        if (exists) {
//            throw new RuntimeException("Seat " + request.getSeatNumber()
//                    + " already exists for flight: " + request.getFlightId());
//        }
//
//        Seat seat = new Seat();
//        seat.setFlightId(request.getFlightId());
//        seat.setSeatNumber(request.getSeatNumber());
//        seat.setSeatClass(request.getSeatClass());
//        seat.setRow(request.getRow());
//        seat.setColumn(request.getColumn());
//        seat.setWindow(request.isWindow());
//        seat.setAisle(request.isAisle());
//        seat.setHasExtraLegroom(request.isHasExtraLegroom());
//        seat.setPriceMultiplier(request.getPriceMultiplier());
//        seat.setStatus("AVAILABLE");
//
//        Seat saved = seatRepository.save(seat);
//        return mapToResponse(saved, "Seat added successfully");
//    }
//
//    @Override
//    public List<SeatResponse> getSeatsByFlight(Long flightId) {
//        return seatRepository.findByFlightId(flightId)
//                .stream()
//                .map(s -> mapToResponse(s, "Success"))
//                .collect(Collectors.toList());
//    }
//
//    // FIX: AVAILABLE + HELD + CONFIRMED sab return karo
//    // Frontend status ke hisaab se color dikhayega — sirf AVAILABLE clickable hogi
//    @Override
//    public List<SeatResponse> getAvailableSeats(Long flightId) {
//        return seatRepository.findByFlightId(flightId)
//                .stream()
//                .map(s -> mapToResponse(s, "Success"))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<SeatResponse> getSeatsByClass(Long flightId, String seatClass) {
//        return seatRepository.findByFlightIdAndSeatClass(flightId, seatClass)
//                .stream()
//                .map(s -> mapToResponse(s, "Success"))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public SeatResponse holdSeat(Long flightId, String seatNumber) {
//        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
//                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));
//
//        if (!seat.getStatus().equals("AVAILABLE")) {
//            throw new RuntimeException("Seat " + seatNumber
//                    + " is not available. Current status: " + seat.getStatus());
//        }
//
//        seat.setStatus("HELD");
//        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
//
//        Seat updated = seatRepository.save(seat);
//        return mapToResponse(updated, "Seat held for 15 minutes. Please complete payment.");
//    }
//
//    @Override
//    public SeatResponse confirmSeat(Long flightId, String seatNumber) {
//        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
//                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));
//
//        // HELD ya already CONFIRMED dono accept karo — idempotent
//        if (seat.getStatus().equals("CONFIRMED")) {
//            return mapToResponse(seat, "Seat already confirmed");
//        }
//
//        if (!seat.getStatus().equals("HELD")) {
//            throw new RuntimeException("Seat " + seatNumber + " is not held. Cannot confirm.");
//        }
//
//        if (seat.getHoldExpiresAt() != null
//                && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
//            seat.setStatus("AVAILABLE");
//            seat.setHoldExpiresAt(null);
//            seatRepository.save(seat);
//            throw new RuntimeException("Seat hold expired. Please select the seat again.");
//        }
//
//        seat.setStatus("CONFIRMED");
//        seat.setHoldExpiresAt(null);
//
//        Seat updated = seatRepository.save(seat);
//        return mapToResponse(updated, "Seat confirmed successfully");
//    }
//
//    @Override
//    public SeatResponse releaseSeat(Long flightId, String seatNumber) {
//        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
//                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));
//
//        if (seat.getStatus().equals("CONFIRMED")) {
//            throw new RuntimeException("Confirmed seat cannot be released directly. Please cancel the booking.");
//        }
//
//        seat.setStatus("AVAILABLE");
//        seat.setHoldExpiresAt(null);
//
//        Seat updated = seatRepository.save(seat);
//        return mapToResponse(updated, "Seat released successfully");
//    }
//
//    @Override
//    public int getAvailableCount(Long flightId) {
//        return seatRepository.countByFlightIdAndStatus(flightId, "AVAILABLE");
//    }
//
//    // Har 2 minute mein expired HELD seats release karo
//    @Scheduled(fixedDelay = 120000)
//    @Override
//    public void releaseExpiredHolds() {
//        List<Seat> expiredSeats = seatRepository
//                .findByStatusAndHoldExpiresAtBefore("HELD", LocalDateTime.now());
//
//        for (Seat seat : expiredSeats) {
//            seat.setStatus("AVAILABLE");
//            seat.setHoldExpiresAt(null);
//            seatRepository.save(seat);
//            System.out.println("Released expired hold — seat: " + seat.getSeatNumber()
//                    + " flight: " + seat.getFlightId());
//        }
//    }
//
//    private SeatResponse mapToResponse(Seat seat, String message) {
//        SeatResponse res = new SeatResponse();
//        res.setId(seat.getId());
//        res.setFlightId(seat.getFlightId());
//        res.setSeatNumber(seat.getSeatNumber());
//        res.setSeatClass(seat.getSeatClass());
//        res.setRow(seat.getRow());
//        res.setColumn(seat.getColumn());
//        res.setWindow(seat.isWindow());
//        res.setAisle(seat.isAisle());
//        res.setHasExtraLegroom(seat.isHasExtraLegroom());
//        res.setStatus(seat.getStatus());
//        res.setPriceMultiplier(seat.getPriceMultiplier());
//        res.setHoldExpiresAt(seat.getHoldExpiresAt());
//        res.setMessage(message);
//        return res;
//    }
//}

package com.skybooker.seat.service;

import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Override
    public SeatResponse addSeat(SeatRequest request) {
        log.info("Adding seat — flightId: {}, seat: {}, class: {}",
                request.getFlightId(), request.getSeatNumber(), request.getSeatClass());

        boolean exists = seatRepository
                .findByFlightIdAndSeatNumber(request.getFlightId(), request.getSeatNumber())
                .isPresent();
        if (exists) {
            log.warn("Seat already exists — flightId: {}, seat: {}",
                    request.getFlightId(), request.getSeatNumber());
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
        log.debug("Seat added — ID: {}, seat: {}", saved.getId(), saved.getSeatNumber());
        return mapToResponse(saved, "Seat added successfully");
    }

    @Override
    public List<SeatResponse> getSeatsByFlight(Long flightId) {
        log.debug("Fetching all seats for flightId: {}", flightId);
        return seatRepository.findByFlightId(flightId)
                .stream().map(s -> mapToResponse(s, "Success")).collect(Collectors.toList());
    }

    @Override
    public List<SeatResponse> getAvailableSeats(Long flightId) {
        log.debug("Fetching seat map for flightId: {}", flightId);
        return seatRepository.findByFlightId(flightId)
                .stream().map(s -> mapToResponse(s, "Success")).collect(Collectors.toList());
    }

    @Override
    public List<SeatResponse> getSeatsByClass(Long flightId, String seatClass) {
        log.debug("Fetching {} class seats for flightId: {}", seatClass, flightId);
        return seatRepository.findByFlightIdAndSeatClass(flightId, seatClass)
                .stream().map(s -> mapToResponse(s, "Success")).collect(Collectors.toList());
    }

    @Override
    public SeatResponse holdSeat(Long flightId, String seatNumber) {
        log.info("Hold request — flightId: {}, seat: {}", flightId, seatNumber);

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> {
                    log.warn("Hold failed — seat not found: {} flightId: {}", seatNumber, flightId);
                    return new RuntimeException("Seat not found: " + seatNumber);
                });

        if (!seat.getStatus().equals("AVAILABLE")) {
            log.warn("Hold failed — seat not available: {} status: {}", seatNumber, seat.getStatus());
            throw new RuntimeException("Seat " + seatNumber
                    + " is not available. Current status: " + seat.getStatus());
        }

        seat.setStatus("HELD");
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        Seat updated = seatRepository.save(seat);
        log.info("Seat held — flightId: {}, seat: {}, expires: {}", flightId, seatNumber, seat.getHoldExpiresAt());
        return mapToResponse(updated, "Seat held for 15 minutes. Please complete payment.");
    }

    @Override
    public SeatResponse confirmSeat(Long flightId, String seatNumber) {
        log.info("Confirm request — flightId: {}, seat: {}", flightId, seatNumber);

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> {
                    log.warn("Confirm failed — seat not found: {} flightId: {}", seatNumber, flightId);
                    return new RuntimeException("Seat not found: " + seatNumber);
                });

        if (seat.getStatus().equals("CONFIRMED")) {
            log.info("Seat already confirmed — flightId: {}, seat: {}", flightId, seatNumber);
            return mapToResponse(seat, "Seat already confirmed");
        }

        if (!seat.getStatus().equals("HELD")) {
            log.warn("Confirm failed — seat not held: {} status: {}", seatNumber, seat.getStatus());
            throw new RuntimeException("Seat " + seatNumber + " is not held. Cannot confirm.");
        }

        if (seat.getHoldExpiresAt() != null
                && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Confirm failed — hold expired for seat: {} flightId: {}", seatNumber, flightId);
            seat.setStatus("AVAILABLE");
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new RuntimeException("Seat hold expired. Please select the seat again.");
        }

        seat.setStatus("CONFIRMED");
        seat.setHoldExpiresAt(null);
        Seat updated = seatRepository.save(seat);
        log.info("Seat confirmed — flightId: {}, seat: {}", flightId, seatNumber);
        return mapToResponse(updated, "Seat confirmed successfully");
    }

    @Override
    public SeatResponse releaseSeat(Long flightId, String seatNumber) {
        log.info("Release request — flightId: {}, seat: {}", flightId, seatNumber);

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));

        if (seat.getStatus().equals("CONFIRMED")) {
            log.warn("Release rejected — seat is CONFIRMED: {} flightId: {}", seatNumber, flightId);
            throw new RuntimeException("Confirmed seat cannot be released directly. Please cancel the booking.");
        }

        seat.setStatus("AVAILABLE");
        seat.setHoldExpiresAt(null);
        Seat updated = seatRepository.save(seat);
        log.info("Seat released — flightId: {}, seat: {}", flightId, seatNumber);
        return mapToResponse(updated, "Seat released successfully");
    }

    @Override
    public int getAvailableCount(Long flightId) {
        int count = seatRepository.countByFlightIdAndStatus(flightId, "AVAILABLE");
        log.debug("Available seat count — flightId: {}, count: {}", flightId, count);
        return count;
    }

    @Scheduled(fixedDelay = 120000)
    @Override
    public void releaseExpiredHolds() {
        List<Seat> expired = seatRepository
                .findByStatusAndHoldExpiresAtBefore("HELD", LocalDateTime.now());

        if (!expired.isEmpty()) {
            log.info("Releasing {} expired seat hold(s)", expired.size());
        }

        for (Seat seat : expired) {
            seat.setStatus("AVAILABLE");
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            log.info("Expired hold released — seat: {}, flightId: {}",
                    seat.getSeatNumber(), seat.getFlightId());
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