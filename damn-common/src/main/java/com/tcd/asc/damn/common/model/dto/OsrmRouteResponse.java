package com.tcd.asc.damn.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class OsrmRouteResponse {
    private String code;
    private List<OsrmRoute> routes;
    private List<OsrmWaypoint> waypoints;
}