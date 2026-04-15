package com.skybooker.airline.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "airports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // Indira Gandhi International Airport

    @Column(unique = true)
    private String iataCode;       // DEL, BOM, BLR

    private String icaoCode;       // VIDP, VABB

    private String city;           // Delhi, Mumbai

    private String country;        // India

    private double latitude;

    private double longitude;

    private String timezone;       // Asia/Kolkata
}
