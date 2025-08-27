package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmWaypoint {
    private String hint;
    private double distance;
    private String name;
    private List<Double> location; // [longitude, latitude]
}