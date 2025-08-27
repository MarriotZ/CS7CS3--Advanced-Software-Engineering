package com.tcd.asc.damn.common.model.dto;

import lombok.Data;

@Data
public class Coordinates {
    private double latitude;
    private double longitude;

    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}