package com.skybooker.seat.controller;

import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // airline staff seat add karega flight ke liye
    @PostMapping
    public ResponseEntity<SeatResponse> addSeat(@RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.addSeat(request));
    }

    // flight ke saare seats dekho - seat map ke liye
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<SeatResponse>> getAllByFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getSeatsByFlight(flightId));
    }

    // sirf available seats dekho
    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<SeatResponse>> getAvailable(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeats(flightId));
    }

    // class ke hisaab se seats filter karo
    @GetMapping("/flight/{flightId}/class/{seatClass}")
    public ResponseEntity<List<SeatResponse>> getByClass(
            @PathVariable Long flightId,
            @PathVariable String seatClass) {
        return ResponseEntity.ok(seatService.getSeatsByClass(flightId, seatClass));
    }

    // seat hold karo - booking shuru karne pe
    @PutMapping("/flight/{flightId}/hold/{seatNumber}")
    public ResponseEntity<SeatResponse> holdSeat(
            @PathVariable Long flightId,
            @PathVariable String seatNumber) {
        return ResponseEntity.ok(seatService.holdSeat(flightId, seatNumber));
    }

    // seat confirm karo - payment hone ke baad
    @PutMapping("/flight/{flightId}/confirm/{seatNumber}")
    public ResponseEntity<SeatResponse> confirmSeat(
            @PathVariable Long flightId,
            @PathVariable String seatNumber) {
        return ResponseEntity.ok(seatService.confirmSeat(flightId, seatNumber));
    }

    // seat release karo - booking cancel ya timeout pe
    @PutMapping("/flight/{flightId}/release/{seatNumber}")
    public ResponseEntity<SeatResponse> releaseSeat(
            @PathVariable Long flightId,
            @PathVariable String seatNumber) {
        return ResponseEntity.ok(seatService.releaseSeat(flightId, seatNumber));
    }

    // available seats ka count
    @GetMapping("/flight/{flightId}/count")
    public ResponseEntity<Integer> getAvailableCount(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getAvailableCount(flightId));
    }
}
