package com.tcd.asc.damn.common.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class OsrmManeuver {
    private String type;
    private String modifier;
    private Integer bearingAfter;
    private Integer bearingBefore;
    private List<Double> location;
}