package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmGeometry {
    private String type;
    private List<List<Double>> coordinates;
}