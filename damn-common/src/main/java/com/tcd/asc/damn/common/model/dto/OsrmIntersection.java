package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmIntersection {
    private List<Double> location;
    private List<Integer> bearings;
    private List<Boolean> entry;
    private Integer in;
    private Integer out;
}