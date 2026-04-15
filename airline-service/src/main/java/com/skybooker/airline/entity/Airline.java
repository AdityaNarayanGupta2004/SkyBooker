package com.skybooker.airline.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "airlines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // IndiGo, Air India

    @Column(unique = true)
    private String iataCode;       // 6E, AI, SG

    private String icaoCode;       // IGO, AIC

    private String country;        // India

    private String contactEmail;

    private String contactPhone;

    // admin activate ya deactivate kar sakta hai
    private boolean isActive;
}
