package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    @Override
    public FlightResponse addFlight(FlightRequest request) {

        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureDate(request.getDepartureDate());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(request.getTotalSeats());
        flight.setPrice(request.getPrice());

        flight.setCreatedAt(LocalDateTime.now());
        flight.setUpdatedAt(LocalDateTime.now());

        Flight saved = flightRepository.save(flight);

        return mapToResponse(saved);
    }

    @Override
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightResponse> searchFlights(String source, String destination, java.time.LocalDate date) {

        return flightRepository
                .findBySourceAndDestinationAndDepartureDate(source, destination, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FlightResponse mapToResponse(Flight flight) {
        FlightResponse res = new FlightResponse();
        res.setId(flight.getId());
        res.setFlightNumber(flight.getFlightNumber());
        res.setAirline(flight.getAirline());
        res.setSource(flight.getSource());
        res.setDestination(flight.getDestination());
        res.setDepartureDate(flight.getDepartureDate());
        res.setDepartureTime(flight.getDepartureTime());
        res.setArrivalTime(flight.getArrivalTime());
        res.setAvailableSeats(flight.getAvailableSeats());
        res.setPrice(flight.getPrice());
        return res;
    }

    @Override
    public String reduceSeats(Long id, int seats){

        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        if(flight.getAvailableSeats() < seats){
            throw new RuntimeException("Not enough seats available");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - seats);

        flightRepository.save(flight);

        return "Seats reduced successfully";
    }
}