package com.tcd.asc.damn.datamonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcd.asc.damn.common.model.dto.Coordinates;
import com.tcd.asc.damn.datamonitor.model.WeatherData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {
    private static final String API_KEY = "756d65a8252648d7b7a73645251104"; // Replace with your key
    private static final String WEATHER_URL = "http://api.weatherapi.com/v1/forecast.json?key=%s&q=%s,%s&days=1&aqi=no&alerts=no";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<WeatherData> getWeatherForCoordinates(List<Coordinates> coordinates) throws Exception {
        List<WeatherData> weatherDataList = new ArrayList<>();
        for (Coordinates coord : coordinates) {
            String url = String.format(WEATHER_URL, API_KEY, coord.getLatitude(), coord.getLongitude());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new java.net.URI(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                JsonNode hourly = json.path("forecast").path("forecastday").get(0).path("hour");
                JsonNode currentHour = hourly.get(0); // Adjust based on travel time
                WeatherData weather = new WeatherData(
                        coord,
                        currentHour.path("temp_c").asDouble(),
                        currentHour.path("condition").path("text").asText(),
                        currentHour.path("precip_mm").asDouble(),
                        currentHour.path("wind_kph").asDouble()
                );
                weatherDataList.add(weather);

                // Save to MongoDB
                mongoTemplate.save(weather, "weather");
            } else {
                System.err.println("Weather API error for " + coord + ": " + response.statusCode());
            }
        }
        return weatherDataList;
    }
}