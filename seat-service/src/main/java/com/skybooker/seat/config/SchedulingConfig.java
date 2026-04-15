package com.skybooker.seat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // yeh class sirf @EnableScheduling ke liye hai
    // SeatServiceImpl mein jo @Scheduled method hai wo isse activate hogi
}
