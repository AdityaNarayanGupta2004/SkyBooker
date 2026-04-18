package com.skybooker.passenger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PassengerResponse {

    private Long passengerId;
    private String bookingId;
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;
    private String seatNumber;
    private String ticketNumber;
    private String passengerType;
    private String message;
}
