package com.skybooker.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {

    private Long id;
    private Long flightId;
    private String seatNumber;
    private String seatClass;
    private int row;
    private String column;
    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;
    private String status;
    private double priceMultiplier;
    private LocalDateTime holdExpiresAt;
    private String message;
}
