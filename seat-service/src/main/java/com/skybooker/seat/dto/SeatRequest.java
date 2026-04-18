package com.skybooker.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatRequest {

    private Long flightId;
    private String seatNumber;   // 12A
    private String seatClass;    // ECONOMY, BUSINESS, FIRST
    private int row;
    private String column;       // A, B, C, D, E, F
    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;
    private double priceMultiplier;  // 1.0 default
}
