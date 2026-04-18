package com.skybooker.airline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirportRequest {

    private String name;
    private String iataCode;
    private String icaoCode;
    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private String timezone;
}
