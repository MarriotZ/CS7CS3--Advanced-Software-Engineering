package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmLeg {
    private List<OsrmStep> steps;
    private double distance;
    private double duration;
    private String summary;
    private double weight;
}