package com.tcd.asc.damn.common.model.response;

import com.tcd.asc.damn.common.constants.TravelMode;
import com.tcd.asc.damn.common.entity.Station;
import lombok.Data;

import java.util.List;

@Data
public class RouteResponse {
    private Station startStation;
    private Station endStation;
    private TravelMode travelMode;
    private Double travelDistance;
    private Double travelCost;
    private Double travelTime;
    private List<String> stationsList;
}