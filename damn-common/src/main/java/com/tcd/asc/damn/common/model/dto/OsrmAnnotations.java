package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmAnnotations {
    private List<Double> distance;
    private List<Double> duration;
    private List<Double> weight;
    private List<Long> nodes;
    private List<Double> speed;
}