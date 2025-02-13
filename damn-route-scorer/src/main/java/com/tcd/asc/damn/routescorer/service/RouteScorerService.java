package com.tcd.asc.damn.routescorer.service;

import com.tcd.asc.damn.common.model.response.RouteResponse;
import org.springframework.stereotype.Service;

@Service
public class RouteScorerService {

    public Double getScore(RouteResponse routeResponse) {
        return routeResponse.getStationsList().size() * 1.0;
    }
}
