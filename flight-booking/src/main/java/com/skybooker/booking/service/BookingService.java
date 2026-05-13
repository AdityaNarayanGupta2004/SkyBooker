package com.skybooker.booking.service;

import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;

public interface BookingService {
    BookingResponse bookFlight(BookingRequest request);
    BookingResponse getBookingById(Long bookingId);
}