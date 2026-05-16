package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.SeatAssignRequest;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private static final String SUCCESS = "Success";
    private static final String PASSENGER_NOT_FOUND_ID = "Passenger not found with id: ";
    private final PassengerRepository passengerRepository;

    @Override
    public PassengerResponse addPassenger(PassengerRequest request) {
        log.info("Adding passenger — bookingId: {}, name: {} {}, type: {}",
                request.getBookingId(), request.getFirstName(),
                request.getLastName(), request.getPassengerType());

        validatePassengerData(request);

        PassengerInfo passenger = new PassengerInfo();
        passenger.setBookingId(request.getBookingId());
        passenger.setTitle(request.getTitle());
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setNationality(request.getNationality());
        passenger.setPassportExpiry(request.getPassportExpiry());
        passenger.setPassengerType(request.getPassengerType());
        passenger.setTicketNumber(generateTicketNumber());
        passenger.setCreatedAt(LocalDateTime.now());

        PassengerInfo saved = passengerRepository.save(passenger);
        log.info("Passenger added — ticketNumber: {}, bookingId: {}",
                saved.getTicketNumber(), saved.getBookingId());
        return mapToResponse(saved, "Passenger added successfully");
    }

    @Override
    public PassengerResponse getPassengerById(Long passengerId) {
        log.debug("Fetching passenger — id: {}", passengerId);
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> {
                    log.warn("Passenger not found — id: {}", passengerId);
                    return new IllegalArgumentException(PASSENGER_NOT_FOUND_ID + passengerId);
                });
        return mapToResponse(passenger, SUCCESS);
    }

    @Override
    public List<PassengerResponse> getPassengersByBooking(String bookingId) {
        log.debug("Fetching passengers for bookingId: {}", bookingId);
        return passengerRepository.findByBookingId(bookingId).stream()
                .map(p -> mapToResponse(p, SUCCESS))
                .toList();
    }

    @Override
    public PassengerResponse getByPassportNumber(String passportNumber) {
        log.debug("Fetching passenger by passport: {}", passportNumber);
        PassengerInfo passenger = passengerRepository.findByPassportNumber(passportNumber)
                .orElseThrow(() -> {
                    log.warn("Passenger not found — passport: {}", passportNumber);
                    return new IllegalArgumentException("Passenger not found with passport: " + passportNumber);
                });
        return mapToResponse(passenger, SUCCESS);
    }

    @Override
    public PassengerResponse getByTicketNumber(String ticketNumber) {
        log.debug("Fetching passenger by ticket: {}", ticketNumber);
        PassengerInfo passenger = passengerRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> {
                    log.warn("Ticket not found: {}", ticketNumber);
                    return new IllegalArgumentException("Ticket not found: " + ticketNumber);
                });
        return mapToResponse(passenger, SUCCESS);
    }

    @Override
    public PassengerResponse updatePassenger(Long passengerId, PassengerRequest request) {
        log.info("Updating passenger — id: {}", passengerId);
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + passengerId));

        passenger.setTitle(request.getTitle());
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setNationality(request.getNationality());
        passenger.setPassportExpiry(request.getPassportExpiry());
        passenger.setPassengerType(request.getPassengerType());

        PassengerInfo updated = passengerRepository.save(passenger);
        log.info("Passenger updated — id: {}", passengerId);
        return mapToResponse(updated, "Passenger updated successfully");
    }

    @Override
    public PassengerResponse assignSeat(SeatAssignRequest request) {
        log.info("Assigning seat — passengerId: {}, seat: {}", request.getPassengerId(), request.getSeatNumber());

        PassengerInfo passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new IllegalArgumentException(PASSENGER_NOT_FOUND_ID + request.getPassengerId()));

        boolean seatTaken = passengerRepository.findBySeatId(request.getSeatId()).isPresent();
        if (seatTaken) {
            log.warn("Seat assignment failed — seat already taken: {}", request.getSeatNumber());
            throw new IllegalStateException("Seat " + request.getSeatNumber() + " is already assigned to another passenger");
        }

        passenger.setSeatId(request.getSeatId());
        passenger.setSeatNumber(request.getSeatNumber());
        PassengerInfo updated = passengerRepository.save(passenger);
        log.info("Seat assigned — passengerId: {}, seat: {}", request.getPassengerId(), request.getSeatNumber());
        return mapToResponse(updated, "Seat assigned successfully");
    }

    @Override
    public void deletePassenger(Long passengerId) {
        log.info("Deleting passenger — id: {}", passengerId);
        if (!passengerRepository.existsById(passengerId)) {
            log.warn("Delete failed — passenger not found: {}", passengerId);
            throw new IllegalArgumentException(PASSENGER_NOT_FOUND_ID + passengerId);
        }
        passengerRepository.deleteById(passengerId);
        log.info("Passenger deleted — id: {}", passengerId);
    }

    @Override
    public void deleteByBookingId(String bookingId) {
        log.info("Deleting all passengers for bookingId: {}", bookingId);
        passengerRepository.deleteByBookingId(bookingId);
    }

    @Override
    public int getPassengerCount(String bookingId) {
        int count = passengerRepository.countByBookingId(bookingId);
        log.debug("Passenger count — bookingId: {}, count: {}", bookingId, count);
        return count;
    }

    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validatePassengerData(PassengerRequest request) {
        if (request.getPassportExpiry() != null &&
                request.getPassportExpiry().isBefore(LocalDate.now())) {
            log.warn("Validation failed — passport expired for: {} {}",
                    request.getFirstName(), request.getLastName());
            throw new IllegalArgumentException("Passport is expired for passenger: "
                    + request.getFirstName() + " " + request.getLastName());
        }

        if ("INFANT".equalsIgnoreCase(request.getPassengerType()) &&
                request.getDateOfBirth() != null) {
            LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
            if (request.getDateOfBirth().isBefore(twoYearsAgo)) {
                log.warn("Validation failed — infant age exceeds 2 years");
                throw new IllegalArgumentException("Infant must be under 2 years of age");
            }
        }

        if ("CHILD".equalsIgnoreCase(request.getPassengerType()) &&
                request.getDateOfBirth() != null) {
            LocalDate twelveYearsAgo = LocalDate.now().minusYears(12);
            if (request.getDateOfBirth().isBefore(twelveYearsAgo)) {
                log.warn("Validation failed — child age exceeds 12 years");
                throw new IllegalArgumentException("Child passenger must be under 12 years of age");
            }
        }
    }

    private PassengerResponse mapToResponse(PassengerInfo passenger, String message) {
        PassengerResponse res = new PassengerResponse();
        res.setPassengerId(passenger.getPassengerId());
        res.setBookingId(passenger.getBookingId());
        res.setTitle(passenger.getTitle());
        res.setFirstName(passenger.getFirstName());
        res.setLastName(passenger.getLastName());
        res.setDateOfBirth(passenger.getDateOfBirth());
        res.setGender(passenger.getGender());
        res.setPassportNumber(passenger.getPassportNumber());
        res.setNationality(passenger.getNationality());
        res.setPassportExpiry(passenger.getPassportExpiry());
        res.setSeatNumber(passenger.getSeatNumber());
        res.setTicketNumber(passenger.getTicketNumber());
        res.setPassengerType(passenger.getPassengerType());
        res.setMessage(message);
        return res;
    }
}
