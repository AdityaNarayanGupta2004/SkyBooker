package com.skybooker.booking.controller;

import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse bookFlight(
            @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String token
    ) {
        // token ko request object mein set karo taaki service flight-service ko forward kar sake
        request.setToken(token);
        return bookingService.bookFlight(request);
    }
}
