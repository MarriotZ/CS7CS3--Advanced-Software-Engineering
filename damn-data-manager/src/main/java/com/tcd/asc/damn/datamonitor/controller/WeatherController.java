package com.tcd.asc.damn.datamonitor.controller;

import com.tcd.asc.damn.common.model.dto.Coordinates;
import com.tcd.asc.damn.datamonitor.model.WeatherData;
import com.tcd.asc.damn.datamonitor.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    @Autowired
    private WeatherService weatherService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/fetch")
    public List<WeatherData> fetchWeather(@RequestBody List<Coordinates> coordinates) throws Exception {
        return weatherService.getWeatherForCoordinates(coordinates);
    }

    @GetMapping("/health")
    public String health() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return "MongoDB is up";
        } catch (Exception e) {
            return "MongoDB is down: " + e.getMessage();
        }
    }
}