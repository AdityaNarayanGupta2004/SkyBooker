package com.skybooker.passenger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passengerId;

    // booking-service se linked
    private String bookingId;

    private String title;           // Mr, Mrs, Ms, Dr
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;          // MALE, FEMALE, OTHER

    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;

    // seat assign hone ke baad fill hoga (web check-in pe)
    private Long seatId;
    private String seatNumber;      // e.g. 12A

    // generated at booking time
    private String ticketNumber;

    // ADULT, CHILD, INFANT — fare calculation ke liye
    private String passengerType;

    private LocalDateTime createdAt;
}
