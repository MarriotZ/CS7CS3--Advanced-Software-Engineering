package com.tcd.asc.damn.common.model.response;

import com.tcd.asc.damn.common.entity.Location;
import lombok.Data;

import java.util.List;

@Data
public class RoutesResponse {
    private Location startLocation;
    private Location endLocation;
    private List<RouteResponse> routes;
}
