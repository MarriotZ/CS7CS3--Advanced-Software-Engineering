package com.tcd.asc.damn.routeprovider.service;

import com.tcd.asc.damn.common.constants.RouteType;
import com.tcd.asc.damn.common.entity.DublinBikeStation;
import com.tcd.asc.damn.common.entity.Stop;
import com.tcd.asc.damn.common.model.dto.Coordinates;
import com.tcd.asc.damn.common.model.dto.RouteSegment;
import com.tcd.asc.damn.common.model.dto.WalkSegment;
import com.tcd.asc.damn.common.model.request.RouteRequest;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.routeprovider.utils.AlphanumericGenerator;
import com.tcd.asc.damn.routeprovider.utils.TransitUtil;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransitService {

    @Autowired
    private Driver neo4jDriver;
    @Autowired
    private AlphanumericGenerator alphanumericGenerator;
    @Autowired
    private TransitUtil transitUtil;
    @Autowired
    private Neo4jService neo4jService;
    @Autowired
    private OsrmService osrmService;

    private static final int NEAREST_BUS_STOPS_LIMIT = 2;
    private static final int NEAREST_CYCLE_STOPS_LIMIT = 1;

    public List<RouteResponse> findPubTransRoutes(RouteRequest routeRequest) {
        double startLat = routeRequest.getStartLocation().getLatitude();
        double startLon = routeRequest.getStartLocation().getLongitude();
        double endLat = routeRequest.getEndLocation().getLatitude();
        double endLon = routeRequest.getEndLocation().getLongitude();

        System.out.println("Finding routes from (" + startLat + ", " + startLon + ") to (" + endLat + ", " + endLon + ")");

        try (Session session = neo4jDriver.session()) {
            List<Stop> startStops = transitUtil.findNearestLuasStops(startLat, startLon, NEAREST_BUS_STOPS_LIMIT);
            List<Stop> endStops = transitUtil.findNearestLuasStops(endLat, endLon, NEAREST_BUS_STOPS_LIMIT);

            List<RouteResponse> routeResponses = new ArrayList<>();
            StringBuilder errorDetails = new StringBuilder();

            for (Stop startStop : startStops) {
                for (Stop endStop : endStops) {
                    List<RouteSegment> transitSegments = neo4jService.findPubTransitRouteBetweenStops(startStop, endStop, session, errorDetails);
                    if (transitSegments != null && !transitSegments.isEmpty()) {
                        RouteResponse routeResponse = new RouteResponse();
                        routeResponse.setRouteId(alphanumericGenerator.generateAlphanumericString());
                        routeResponse.setRouteType(RouteType.TRANSIT);
                        List<RouteSegment> routeSegments = new ArrayList<>();

                        // Add initial walk segment from start location to boarding stop
                        /*WalkSegment initialWalk = new WalkSegment();
                        initialWalk.setStartCoordinate(routeRequest.getStartLocation());
                        initialWalk.setEndCoordinate(new Coordinates(startStop.getStopLat(), startStop.getStopLon()));
                        initialWalk.setWalkPath(
                                osrmService.getWalkPath(
                                        routeRequest.getStartLocation(),
                                        new Coordinates(startStop.getStopLat(), startStop.getStopLon())
                                )
                        );
                        routeSegments.add(initialWalk);*/

                        routeSegments.add(
                                osrmService.getWalkSegment(
                                    routeRequest.getStartLocation(),
                                    new Coordinates(startStop.getStopLat(), startStop.getStopLon())
                                )
                        );

                        // Add transit segments
                        routeSegments.addAll(transitSegments);

                        // Add final walk segment from alighting stop to end location
                        /*WalkSegment finalWalk = new WalkSegment();
                        finalWalk.setStartCoordinate(new Coordinates(endStop.getStopLat(), endStop.getStopLon()));
                        finalWalk.setEndCoordinate(routeRequest.getEndLocation());
                        finalWalk.setWalkPath(
                                osrmService.getWalkPath(
                                        new Coordinates(endStop.getStopLat(), endStop.getStopLon()),
                                        routeRequest.getEndLocation()
                                )
                        );
                        routeSegments.add(finalWalk);*/

                        routeSegments.add(
                                osrmService.getWalkSegment(
                                        new Coordinates(endStop.getStopLat(), endStop.getStopLon()),
                                        routeRequest.getEndLocation()
                                )
                        );

                        routeResponse.setRoutes(routeSegments);
                        routeResponse.setUniqueTransitTypes(routeSegments.stream().map(RouteSegment::getTransitType).distinct().toList());
                        routeResponses.add(routeResponse);
                    }
                }
            }

            if (routeResponses.isEmpty()) {
                String errorMessage = "No routes found between any start and end stops. " +
                        "Start stops: " + startStops.stream().map(s -> s.getStopName() + " (" + s.getStopId() + ")").collect(Collectors.joining(", ")) +
                        ". End stops: " + endStops.stream().map(s -> s.getStopName() + " (" + s.getStopId() + ")").collect(Collectors.joining(", ")) +
                        ". ";
                System.out.println(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            System.out.println("Found " + routeResponses.size() + " possible routes with ids - " + routeResponses.stream().map(RouteResponse::getRouteId).collect(Collectors.joining(", ")));
            List<RouteResponse> bestRoute = transitUtil.filterBestRoute(routeResponses);
            System.out.println("Best route found with " + bestRoute.size() + " stops is " + bestRoute.get(0).getRouteId());
            return bestRoute;
        } catch (Exception e) {
            System.err.println("Error finding routes: " + e.getMessage());
            throw new RuntimeException("Failed to find routes: " + e.getMessage(), e);
        }
    }

    public List<RouteResponse> getDrivingRoutes(RouteRequest routeRequest) {
        return osrmService.getDrivingRoute(routeRequest.getStartLocation(), routeRequest.getEndLocation());
    }

    public List<RouteResponse> getDubCycleRoutes(RouteRequest routeRequest) {
        DublinBikeStation nearestStartCycleStop = transitUtil.findNearestCycleStop(routeRequest.getStartLocation().getLatitude(), routeRequest.getStartLocation().getLongitude(), NEAREST_CYCLE_STOPS_LIMIT);
        DublinBikeStation nearesteEndCycleStop = transitUtil.findNearestCycleStop(routeRequest.getEndLocation().getLatitude(), routeRequest.getEndLocation().getLongitude(), NEAREST_CYCLE_STOPS_LIMIT);
        List<RouteResponse> routeResponses = new ArrayList<>();

        if (nearestStartCycleStop != null && nearesteEndCycleStop != null) {
            List<RouteSegment> cycleSegments = new ArrayList<>(osrmService.getCycleRoute(nearestStartCycleStop, nearesteEndCycleStop));

            RouteResponse routeResponse = new RouteResponse();
            routeResponse.setRouteId(alphanumericGenerator.generateAlphanumericString());
            routeResponse.setRouteType(RouteType.CYCLING);
            List<RouteSegment> routeSegments = new ArrayList<>();

            // Add initial walk segment from start location to boarding stop
            /*WalkSegment initialWalk = new WalkSegment();
            initialWalk.setStartCoordinate(routeRequest.getStartLocation());
            initialWalk.setEndCoordinate(new Coordinates(nearestStartCycleStop.getLatitude(), nearestStartCycleStop.getLongitude()));
            initialWalk.setWalkPath(
                    osrmService.getWalkPath(
                            routeRequest.getStartLocation(),
                            new Coordinates(nearestStartCycleStop.getLatitude(), nearestStartCycleStop.getLongitude())
                    )
            );
            routeSegments.add(initialWalk);*/

            routeSegments.add(
                    osrmService.getWalkSegment(
                            routeRequest.getStartLocation(),
                            new Coordinates(nearestStartCycleStop.getLatitude(), nearestStartCycleStop.getLongitude())
                    )
            );

            // Add transit segments
            routeSegments.addAll(cycleSegments);

            // Add final walk segment from alighting stop to end location
            /*WalkSegment finalWalk = new WalkSegment();
            finalWalk.setStartCoordinate(new Coordinates(nearesteEndCycleStop.getLatitude(), nearesteEndCycleStop.getLongitude()));
            finalWalk.setEndCoordinate(routeRequest.getEndLocation());
            finalWalk.setWalkPath(
                    osrmService.getWalkPath(
                            new Coordinates(nearesteEndCycleStop.getLatitude(), nearesteEndCycleStop.getLongitude()),
                            routeRequest.getEndLocation()
                    )
            );
            routeSegments.add(finalWalk);*/

            routeSegments.add(
                    osrmService.getWalkSegment(
                            new Coordinates(nearesteEndCycleStop.getLatitude(), nearesteEndCycleStop.getLongitude()),
                            routeRequest.getEndLocation()
                    )
            );

            routeResponse.setRoutes(routeSegments);
            routeResponse.setUniqueTransitTypes(routeSegments.stream().map(RouteSegment::getTransitType).distinct().toList());
            routeResponses.add(routeResponse);
            return routeResponses;
        } else {
            System.out.println("No cycle routes found between the given locations.");
            return List.of();
        }
    }

    public List<RouteResponse> getWalkingRoutes(RouteRequest routeRequest) {
        return osrmService.getWalkingRoute(routeRequest.getStartLocation(), routeRequest.getEndLocation());
    }
}