package com.skybooker.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String message;
    private boolean success;
    private String source;
    private String destination;
    private String departureDate;
    private String departureTime;
    private String airline;
}