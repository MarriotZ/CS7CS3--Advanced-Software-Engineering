package com.tcd.asc.damn.datamonitor.model;

import com.tcd.asc.damn.common.model.dto.Coordinates;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = "weather")
@Data
public class WeatherData implements Serializable {
    @Id
    private String id; // Unique ID (e.g., latitude_longitude)

    @Indexed
    private Coordinates coordinates;
    private double temperature;
    private String condition;
    private double precipitation;
    private double windSpeed;

    // Constructor
    public WeatherData(Coordinates coordinates, double temperature, String condition,
                       double precipitation, double windSpeed) {
        this.coordinates = coordinates;
        this.temperature = temperature;
        this.condition = condition;
        this.precipitation = precipitation;
        this.windSpeed = windSpeed;
        this.id = coordinates.getLatitude() + "_" + coordinates.getLongitude();
    }
}