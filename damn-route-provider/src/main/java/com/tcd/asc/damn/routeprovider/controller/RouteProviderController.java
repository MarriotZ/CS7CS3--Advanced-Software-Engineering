package com.tcd.asc.damn.routeprovider.controller;


import com.tcd.asc.damn.common.model.request.RouteRequest;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.common.model.response.RoutesResponse;
import com.tcd.asc.damn.routeprovider.service.TransitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/routes-provider")
public class RouteProviderController {

    @Autowired
    private TransitService transitService;

    @PostMapping("/get-routes")
    public ResponseEntity<RoutesResponse> getRoute(@RequestBody RouteRequest routeRequest) {
        System.out.println("Received request to find route from (" +
                routeRequest.getStartLocation().getLatitude() + ", " +
                routeRequest.getStartLocation().getLongitude() + ") to (" +
                routeRequest.getEndLocation().getLatitude() + ", " +
                routeRequest.getEndLocation().getLongitude() + ")");

        List<RouteResponse> routes = new ArrayList<>();

        routes.addAll(transitService.findPubTransRoutes(routeRequest));
        routes.addAll(transitService.getDrivingRoutes(routeRequest));
        routes.addAll(transitService.getDubCycleRoutes(routeRequest));
        routes.addAll(transitService.getWalkingRoutes(routeRequest));

        RoutesResponse routesResponse = new RoutesResponse(routeRequest.getStartLocation(), routeRequest.getEndLocation(), routes.size(), routes);
        return ResponseEntity.ok(routesResponse);
    }

}
