package com.tcd.asc.damn.routeprovider.controller;


import com.tcd.asc.damn.common.model.request.RouteRequest;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.routeprovider.service.RouteProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes-provider")
public class RouteProviderController {

    @Autowired
    private RouteProviderService routeProviderService;

    @PostMapping("/get-route")
    public List<RouteResponse> getRoute(@RequestBody RouteRequest routeRequest) {
        List<RouteResponse> routesResponse = routeProviderService.getRoutes(routeRequest.getStartLocation(), routeRequest.getEndLocation());
        return routesResponse;
    }
}
