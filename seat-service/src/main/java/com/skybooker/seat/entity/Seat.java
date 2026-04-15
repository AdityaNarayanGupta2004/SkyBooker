package com.skybooker.seat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // kis flight ka seat hai
    private Long flightId;

    // seat number jaise 12A, 14C
    private String seatNumber;

    // ECONOMY, BUSINESS, FIRST
    private String seatClass;

    // row number - 1, 2, 3...
    // 'row' MySQL ka reserved word hai isliye column name alag rakha
    @Column(name = "seat_row")
    private int row;

    // column - A, B, C, D, E, F
    // 'column' bhi MySQL ka reserved word hai isliye rename kiya
    @Column(name = "seat_column")
    private String column;

    // window seat hai ya nahi
    private boolean isWindow;

    // aisle seat hai ya nahi
    private boolean isAisle;

    // extra legroom hai ya nahi
    private boolean hasExtraLegroom;

    // AVAILABLE, HELD, CONFIRMED, BLOCKED
    private String status;

    // base fare pe multiplier - 1.0 normal, 1.2 extra legroom, 1.5 business
    private double priceMultiplier;

    // hold kab expire hogi - 15 min baad
    private LocalDateTime holdExpiresAt;
}
