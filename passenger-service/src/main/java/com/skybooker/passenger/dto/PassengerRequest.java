package com.skybooker.passenger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PassengerRequest {

    private String bookingId;
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;
    private String passengerType;   // ADULT, CHILD, INFANT
}
