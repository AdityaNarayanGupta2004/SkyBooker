package com.skybooker.airline.controller;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    // ==================== AIRLINE ENDPOINTS ====================

    // nai airline add karo - admin karega
    @PostMapping("/airlines")
    public ResponseEntity<AirlineResponse> addAirline(@RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.addAirline(request));
    }

    // id se airline lo
    @GetMapping("/airlines/{id}")
    public ResponseEntity<AirlineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    // IATA code se airline lo - jaise 6E, AI
    @GetMapping("/airlines/iata/{iataCode}")
    public ResponseEntity<AirlineResponse> getByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirlineByIata(iataCode));
    }

    // saari airlines
    @GetMapping("/airlines")
    public ResponseEntity<List<AirlineResponse>> getAll() {
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    // sirf active airlines - flight search mein use hoga
    @GetMapping("/airlines/active")
    public ResponseEntity<List<AirlineResponse>> getActive() {
        return ResponseEntity.ok(airlineService.getActiveAirlines());
    }

    // airline update karo
    @PutMapping("/airlines/{id}")
    public ResponseEntity<AirlineResponse> update(
            @PathVariable Long id,
            @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.updateAirline(id, request));
    }

    // activate ya deactivate toggle karo
    @PutMapping("/airlines/{id}/toggle-status")
    public ResponseEntity<AirlineResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.toggleAirlineStatus(id));
    }

    // ==================== AIRPORT ENDPOINTS ====================

    // naya airport add karo - admin karega
    @PostMapping("/airports")
    public ResponseEntity<AirportResponse> addAirport(@RequestBody AirportRequest request) {
        return ResponseEntity.ok(airlineService.addAirport(request));
    }

    // id se airport lo
    @GetMapping("/airports/{id}")
    public ResponseEntity<AirportResponse> getAirportById(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getAirportById(id));
    }

    // IATA code se airport lo - jaise DEL, BOM
    @GetMapping("/airports/iata/{iataCode}")
    public ResponseEntity<AirportResponse> getAirportByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirportByIata(iataCode));
    }

    // saare airports
    @GetMapping("/airports")
    public ResponseEntity<List<AirportResponse>> getAllAirports() {
        return ResponseEntity.ok(airlineService.getAllAirports());
    }

    // city ya name se search - flight form autocomplete ke liye
    @GetMapping("/airports/search")
    public ResponseEntity<List<AirportResponse>> searchAirports(@RequestParam String keyword) {
        return ResponseEntity.ok(airlineService.searchAirports(keyword));
    }

    // airport update karo
    @PutMapping("/airports/{id}")
    public ResponseEntity<AirportResponse> updateAirport(
            @PathVariable Long id,
            @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airlineService.updateAirport(id, request));
    }
}
