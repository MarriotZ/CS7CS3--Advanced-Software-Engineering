package com.tcd.asc.damn.datamonitor.controller;

import com.tcd.asc.damn.common.entity.DublinBikeStation;
import com.tcd.asc.damn.common.repository.DublinBikeStationRepository;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DublinBikesRoute extends RouteBuilder {

    @Autowired
    private DublinBikeStationRepository stationRepository;

    @Override
    public void configure() throws Exception {
        // Configure Jackson to unmarshal JSON into List<DublinBikeStationJson>
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(DublinBikeStationJson.class);
        jacksonDataFormat.setCollectionType(List.class);

        // Define error handling
        onException(Exception.class)
                .handled(true)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    System.err.println("Error fetching bike data: " + exception.getMessage());
                });

        // Define the route
        from("timer:dublinBikes?period=120000")
                .to("https://api.citybik.es/dublinbikes.json")
                .unmarshal(jacksonDataFormat)
                .process(exchange -> {
                    List<DublinBikeStationJson> jsonStations = exchange.getIn().getBody(List.class);

                    // Convert to DublinBikeStation with normalized coordinates
                    List<DublinBikeStation> stations = jsonStations.stream()
                            .map(this::convertToDublinBikeStation)
                            .collect(Collectors.toList());

                    // Save to database
                    stationRepository.saveAll(stations);
                    System.out.println("Saved " + stations.size() + " bike stations to database.");

                    // Set the processed stations back to the exchange body if needed downstream
                    exchange.getIn().setBody(stations);
                });
    }

    private DublinBikeStation convertToDublinBikeStation(DublinBikeStationJson json) {
        DublinBikeStation station = new DublinBikeStation();
        station.setId(json.getId());
        station.setIdx(json.getIdx());
        station.setName(json.getName());
        // Normalize coordinates
        station.setLatitude(normalizeCoordinate(json.getLat()));
        station.setLongitude(normalizeCoordinate(json.getLng()));
        station.setTimestamp(LocalDateTime.parse(json.getTimestamp().replace("Z", "")));
        station.setBikes(json.getBikes());
        station.setFree(json.getFree());
        return station;
    }

    private double normalizeCoordinate(Number value) {
        if (value == null) {
            throw new IllegalArgumentException("Coordinate cannot be null");
        }
        double val = value.doubleValue();
        // Convert microdegrees to decimal degrees if outside valid range
        if (Math.abs(val) > 180) { // Covers lat (>90) and lng (>180)
            return val / 1_000_000.0;
        }
        return val; // Already in decimal degrees
    }
}

// DTO class to match Citybik.es JSON structure
class DublinBikeStationJson {
    private Integer id;
    private Integer idx;
    private String name;
    private Number lat; // Flexible for microdegrees (int/long) or decimal degrees (double)
    private Number lng;
    private String timestamp;
    private Integer bikes;
    private Integer free;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdx() { return idx; }
    public void setIdx(Integer idx) { this.idx = idx; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Number getLat() { return lat; }
    public void setLat(Number lat) { this.lat = lat; }
    public Number getLng() { return lng; }
    public void setLng(Number lng) { this.lng = lng; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Integer getBikes() { return bikes; }
    public void setBikes(Integer bikes) { this.bikes = bikes; }
    public Integer getFree() { return free; }
    public void setFree(Integer free) { this.free = free; }
}