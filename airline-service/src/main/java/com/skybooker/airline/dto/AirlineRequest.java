package com.skybooker.airline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirlineRequest {

    private String name;
    private String iataCode;
    private String icaoCode;
    private String country;
    private String contactEmail;
    private String contactPhone;
}
