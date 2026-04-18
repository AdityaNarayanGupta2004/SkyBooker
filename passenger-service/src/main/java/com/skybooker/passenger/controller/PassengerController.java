package com.skybooker.passenger.controller;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.SeatAssignRequest;
import com.skybooker.passenger.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    // naya passenger add karo — booking ke baad call hoga
    @PostMapping
    public ResponseEntity<PassengerResponse> addPassenger(@RequestBody PassengerRequest request) {
        PassengerResponse response = passengerService.addPassenger(request);
        return ResponseEntity.ok(response);
    }

    // passenger ID se details lo
    @GetMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> getById(@PathVariable Long passengerId) {
        PassengerResponse response = passengerService.getPassengerById(passengerId);
        return ResponseEntity.ok(response);
    }

    // ek booking ke saare passengers lo
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PassengerResponse>> getByBooking(@PathVariable String bookingId) {
        List<PassengerResponse> responses = passengerService.getPassengersByBooking(bookingId);
        return ResponseEntity.ok(responses);
    }

    // passport number se search karo
    @GetMapping("/passport/{passportNumber}")
    public ResponseEntity<PassengerResponse> getByPassport(@PathVariable String passportNumber) {
        PassengerResponse response = passengerService.getByPassportNumber(passportNumber);
        return ResponseEntity.ok(response);
    }

    // ticket number se passenger dhundho
    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<PassengerResponse> getByTicket(@PathVariable String ticketNumber) {
        PassengerResponse response = passengerService.getByTicketNumber(ticketNumber);
        return ResponseEntity.ok(response);
    }

    // passenger details update karo
    @PutMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> updatePassenger(
            @PathVariable Long passengerId,
            @RequestBody PassengerRequest request) {
        PassengerResponse response = passengerService.updatePassenger(passengerId, request);
        return ResponseEntity.ok(response);
    }

    // web check-in ke time seat assign karo
    @PutMapping("/assign-seat")
    public ResponseEntity<PassengerResponse> assignSeat(@RequestBody SeatAssignRequest request) {
        PassengerResponse response = passengerService.assignSeat(request);
        return ResponseEntity.ok(response);
    }

    // single passenger delete karo
    @DeleteMapping("/{passengerId}")
    public ResponseEntity<String> deletePassenger(@PathVariable Long passengerId) {
        passengerService.deletePassenger(passengerId);
        return ResponseEntity.ok("Passenger deleted successfully");
    }

    // booking cancel hone pe saare passengers delete karo
    @DeleteMapping("/booking/{bookingId}")
    public ResponseEntity<String> deleteByBooking(@PathVariable String bookingId) {
        passengerService.deleteByBookingId(bookingId);
        return ResponseEntity.ok("All passengers for booking " + bookingId + " deleted");
    }

    // ek booking mein total passengers count karo
    @GetMapping("/count/{bookingId}")
    public ResponseEntity<Integer> getCount(@PathVariable String bookingId) {
        int count = passengerService.getPassengerCount(bookingId);
        return ResponseEntity.ok(count);
    }
}
