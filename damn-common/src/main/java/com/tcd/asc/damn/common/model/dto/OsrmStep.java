package com.tcd.asc.damn.common.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class OsrmStep {
    private OsrmGeometry osrmGeometry;
    private double distance;
    private double duration;
    private OsrmManeuver maneuver;
    private String name;
    private String mode;
    private double weight;
    private List<OsrmIntersection> intersections;
    private String ref;
    private String rotaryName;
}