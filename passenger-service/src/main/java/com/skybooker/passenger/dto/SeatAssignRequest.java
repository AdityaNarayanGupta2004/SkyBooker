package com.skybooker.passenger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatAssignRequest {
    private Long passengerId;
    private Long seatId;
    private String seatNumber;
}
