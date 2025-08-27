package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmRoute {
    private OsrmGeometry geometry;
    private List<OsrmLeg> legs;
    private double distance;
    private double duration;
    private String weightName;
    private double weight;
    private OsrmAnnotations annotations;
}