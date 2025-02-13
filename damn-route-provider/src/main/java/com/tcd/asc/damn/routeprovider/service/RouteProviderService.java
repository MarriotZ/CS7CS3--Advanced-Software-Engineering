package com.tcd.asc.damn.routeprovider.service;

import com.tcd.asc.damn.common.entity.Location;
import com.tcd.asc.damn.common.entity.Station;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.common.restclient.DataProviderClient;
import com.tcd.asc.damn.common.restclient.RouteScorerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteProviderService {

    @Autowired
    private DataProviderClient dataProviderClient;

    @Autowired
    private RouteScorerClient routeScorerClient;

    public List<RouteResponse> getRoutes(Location startLocation, Location endLocation) {
        List<RouteResponse> routes = new ArrayList<>();
        RouteResponse route = new RouteResponse();

        Station startStation = dataProviderClient.findNearestStation(startLocation);
        Station endStation = dataProviderClient.findNearestStation(endLocation);
        List<String> betweenStations = dataProviderClient.getBetweenStations(startStation.getStationId(), endStation.getStationId());

        route.setStartStation(startStation);
        route.setEndStation(endStation);
        route.setStationsList(betweenStations);

        Double routeScore = routeScorerClient.getRouteScore(route);
        route.setTravelCost(routeScore);
        route.setTravelDistance((double) betweenStations.size());
        routes.add(route);
        return routes;
    }
}
