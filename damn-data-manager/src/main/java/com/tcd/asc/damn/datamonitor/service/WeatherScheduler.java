package com.tcd.asc.damn.datamonitor.service;

import com.tcd.asc.damn.common.model.dto.Coordinates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

//@Component
//@EnableScheduling
public class WeatherScheduler {
//    @Autowired
    private WeatherService weatherService;

    @Scheduled(fixedRate = 3600000) // Every hour
    public void fetchWeather() throws Exception {
        // Example coordinates (replace with dynamic source, e.g., from routes)
        List<Coordinates> coordinates = Arrays.asList(
                new Coordinates(40.7128, -74.0060), // NYC
                new Coordinates(34.0522, -118.2437) // LA
        );
        weatherService.getWeatherForCoordinates(coordinates);
    }
}