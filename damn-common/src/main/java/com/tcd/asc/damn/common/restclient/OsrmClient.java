package com.tcd.asc.damn.common.restclient;

import com.tcd.asc.damn.common.model.dto.OsrmRouteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "osrm-client", url = "http://35.193.190.66:5000")
public interface OsrmClient {

    // Driving mode route
    @GetMapping("/driving/route/v1/driving/{coordinates}")
    OsrmRouteResponse getDrivingRoute(
            @PathVariable("coordinates") String coordinates,
            @RequestParam(value = "overview", required = false, defaultValue = "full") String overview,
            @RequestParam(value = "steps", required = false, defaultValue = "false") Boolean steps,
            @RequestParam(value = "geometries", required = false, defaultValue = "geojson") String geometries,
            @RequestParam(value = "annotations", required = false, defaultValue = "false") Boolean annotations,
            @RequestParam(value = "alternatives", required = false, defaultValue = "false") Boolean alternatives
    );

    // Foot mode route
    @GetMapping("/walking/route/v1/foot/{coordinates}")
    OsrmRouteResponse getFootRoute(
            @PathVariable("coordinates") String coordinates,
            @RequestParam(value = "overview", required = false, defaultValue = "full") String overview,
            @RequestParam(value = "steps", required = false, defaultValue = "false") Boolean steps,
            @RequestParam(value = "geometries", required = false, defaultValue = "geojson") String geometries
    );

    // Bike mode route
    @GetMapping("/biking/route/v1/bicycle/{coordinates}")
    OsrmRouteResponse getBikeRoute(
            @PathVariable("coordinates") String coordinates,
            @RequestParam(value = "overview", required = false, defaultValue = "full") String overview,
            @RequestParam(value = "steps", required = false, defaultValue = "false") Boolean steps,
            @RequestParam(value = "geometries", required = false, defaultValue = "geojson") String geometries
    );
}