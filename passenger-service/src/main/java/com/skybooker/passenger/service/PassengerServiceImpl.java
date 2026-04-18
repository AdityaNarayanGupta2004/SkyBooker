package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.SeatAssignRequest;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    public PassengerResponse addPassenger(PassengerRequest request) {

        // pehle validate karo passenger data
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
        return mapToResponse(saved, "Passenger added successfully");
    }

    @Override
    public PassengerResponse getPassengerById(Long passengerId) {
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));
        return mapToResponse(passenger, "Success");
    }

    @Override
    public List<PassengerResponse> getPassengersByBooking(String bookingId) {
        List<PassengerInfo> passengers = passengerRepository.findByBookingId(bookingId);
        return passengers.stream()
                .map(p -> mapToResponse(p, "Success"))
                .collect(Collectors.toList());
    }

    @Override
    public PassengerResponse getByPassportNumber(String passportNumber) {
        PassengerInfo passenger = passengerRepository.findByPassportNumber(passportNumber)
                .orElseThrow(() -> new RuntimeException("Passenger not found with passport: " + passportNumber));
        return mapToResponse(passenger, "Success");
    }

    @Override
    public PassengerResponse getByTicketNumber(String ticketNumber) {
        PassengerInfo passenger = passengerRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));
        return mapToResponse(passenger, "Success");
    }

    @Override
    public PassengerResponse updatePassenger(Long passengerId, PassengerRequest request) {
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

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
        return mapToResponse(updated, "Passenger updated successfully");
    }

    @Override
    public PassengerResponse assignSeat(SeatAssignRequest request) {
        PassengerInfo passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + request.getPassengerId()));

        // check karo koi aur passenger is seat pe already assign toh nahi hai
        boolean seatTaken = passengerRepository.findBySeatId(request.getSeatId()).isPresent();
        if (seatTaken) {
            throw new RuntimeException("Seat " + request.getSeatNumber() + " is already assigned to another passenger");
        }

        passenger.setSeatId(request.getSeatId());
        passenger.setSeatNumber(request.getSeatNumber());

        PassengerInfo updated = passengerRepository.save(passenger);
        return mapToResponse(updated, "Seat assigned successfully");
    }

    @Override
    public void deletePassenger(Long passengerId) {
        if (!passengerRepository.existsById(passengerId)) {
            throw new RuntimeException("Passenger not found with id: " + passengerId);
        }
        passengerRepository.deleteById(passengerId);
    }

    @Override
    public void deleteByBookingId(String bookingId) {
        passengerRepository.deleteByBookingId(bookingId);
    }

    @Override
    public int getPassengerCount(String bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    // ticket number generate karo — format: TKT-XXXXXXXX (unique)
    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // passenger data validate karo booking confirm karne se pehle
    private void validatePassengerData(PassengerRequest request) {

        // passport expiry check
        if (request.getPassportExpiry() != null &&
                request.getPassportExpiry().isBefore(LocalDate.now())) {
            throw new RuntimeException("Passport is expired for passenger: "
                    + request.getFirstName() + " " + request.getLastName());
        }

        // INFANT ke liye age check — 2 saal se upar nahi hona chahiye
        if ("INFANT".equalsIgnoreCase(request.getPassengerType()) &&
                request.getDateOfBirth() != null) {
            LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
            if (request.getDateOfBirth().isBefore(twoYearsAgo)) {
                throw new RuntimeException("Infant must be under 2 years of age");
            }
        }

        // CHILD ke liye age check — 2 se 12 saal ke beech
        if ("CHILD".equalsIgnoreCase(request.getPassengerType()) &&
                request.getDateOfBirth() != null) {
            LocalDate twelveYearsAgo = LocalDate.now().minusYears(12);
            if (request.getDateOfBirth().isBefore(twelveYearsAgo)) {
                throw new RuntimeException("Child passenger must be under 12 years of age");
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
