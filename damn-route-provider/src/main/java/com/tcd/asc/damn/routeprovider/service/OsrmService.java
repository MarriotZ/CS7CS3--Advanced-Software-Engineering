package com.tcd.asc.damn.routeprovider.service;

import com.tcd.asc.damn.common.constants.RouteType;
import com.tcd.asc.damn.common.constants.TransitType;
import com.tcd.asc.damn.common.entity.DublinBikeStation;
import com.tcd.asc.damn.common.model.dto.*;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.common.restclient.OsrmClient;
import com.tcd.asc.damn.routeprovider.utils.CyclingSegmentMapper;
import com.tcd.asc.damn.routeprovider.utils.DriveSegmentMapper;
import com.tcd.asc.damn.routeprovider.utils.TransitUtil;
import com.tcd.asc.damn.routeprovider.utils.WalkSegmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OsrmService {

    @Autowired
    private OsrmClient osrmClient;
    @Autowired
    private TransitUtil transitUtil;

    WalkSegment getWalkSegment(Coordinates start, Coordinates end) {
        String coordinates = getCoordinatesString(start, end);
        try {
            OsrmRouteResponse osrmRouteResponse = osrmClient.getFootRoute(coordinates, "full", true, "geojson");
            if (osrmRouteResponse != null && osrmRouteResponse.getRoutes() != null && !osrmRouteResponse.getRoutes().isEmpty()) {
                return WalkSegmentMapper.createWalkSegments(osrmRouteResponse).get(0);
            } else {
                System.out.println("OSRM returned null or empty walking routes for coordinates: " + coordinates);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error calling OSRM walking server for coordinates " + coordinates + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    List<Coordinates> getWalkPath(Coordinates start, Coordinates end) {
        String coordinates = getCoordinatesString(start, end);
        try {
            OsrmRouteResponse osrmRouteResponse = osrmClient.getFootRoute(coordinates, "full", true, "geojson");
            if (osrmRouteResponse != null && osrmRouteResponse.getRoutes() != null && !osrmRouteResponse.getRoutes().isEmpty()) {
                List<List<Double>> geoJsonCoords = osrmRouteResponse.getRoutes().get(0).getGeometry().getCoordinates();
                return geoJsonCoords.stream()
                        .map(coord -> new Coordinates(coord.get(1), coord.get(0)))
                        .collect(Collectors.toList());
            } else {
                System.out.println("OSRM returned null or empty walking routes for coordinates: " + coordinates);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Error calling OSRM walking server for coordinates " + coordinates + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<RouteResponse> getDrivingRoute(Coordinates start, Coordinates end) {
        String coordinates = getCoordinatesString(start, end);
        List<RouteResponse> routeResponses = new ArrayList<>();

        try {
            OsrmRouteResponse osrmResponse = osrmClient.getDrivingRoute(coordinates, "full", true, "geojson", true, true);
            if (osrmResponse != null && osrmResponse.getRoutes() != null && !osrmResponse.getRoutes().isEmpty()) {
                List<DriveSegment> segments = DriveSegmentMapper.createDriveSegments(osrmResponse);
                for (DriveSegment segment : segments) {
                    routeResponses.add(transitUtil.convertToRouteResponse(segment, RouteType.DRIVING, TransitType.DRIVING));
                }
            } else {
                System.out.println("OSRM returned null or empty driving routes for coordinates: " + coordinates);
            }
        } catch (Exception e) {
            System.err.println("Error calling OSRM driving server for coordinates " + coordinates + ": " + e.getMessage());
            e.printStackTrace();
        }

        return routeResponses;
    }

    private static String getCoordinatesString(Coordinates start, Coordinates end) {
        return start.getLongitude() + "," + start.getLatitude() + ";" +
                end.getLongitude() + "," + end.getLatitude();
    }

    public List<CyclingSegment> getCycleRoute(DublinBikeStation startDockStation, DublinBikeStation endDockStation) {
        String coordinates = getCoordinatesString(
                new Coordinates(startDockStation.getLatitude(), startDockStation.getLongitude()),
                new Coordinates(endDockStation.getLatitude(), endDockStation.getLongitude())
        );
        try {
            OsrmRouteResponse osrmResponse = osrmClient.getBikeRoute(coordinates, "full", true, "geojson");
            if (osrmResponse != null && osrmResponse.getRoutes() != null && !osrmResponse.getRoutes().isEmpty()) {
                return CyclingSegmentMapper.createCyclingSegments(osrmResponse, startDockStation, endDockStation);
            } else {
                System.out.println("OSRM returned null or empty cycling routes for coordinates: " + coordinates);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Error calling OSRM cycling server for coordinates " + coordinates + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<RouteResponse> getWalkingRoute(Coordinates startLocation, Coordinates endLocation) {
        String coordinates = getCoordinatesString(startLocation, endLocation);
        List<RouteResponse> routeResponses = new ArrayList<>();

        try {
            OsrmRouteResponse osrmResponse = osrmClient.getFootRoute(coordinates, "full", true, "geojson");
            if (osrmResponse != null && osrmResponse.getRoutes() != null && !osrmResponse.getRoutes().isEmpty()) {
                List<WalkSegment> segments = WalkSegmentMapper.createWalkSegments(osrmResponse);
                for (WalkSegment segment : segments) {
                    routeResponses.add(transitUtil.convertToRouteResponse(segment, RouteType.WALKING, TransitType.WALK));
                }
            } else {
                System.out.println("OSRM returned null or empty driving routes for coordinates: " + coordinates);
            }
        } catch (Exception e) {
            System.err.println("Error calling OSRM driving server for coordinates " + coordinates + ": " + e.getMessage());
            e.printStackTrace();
        }

        return routeResponses;
    }
}
