package com.tcd.asc.damn.routeprovider.utils;

import com.tcd.asc.damn.common.constants.RouteType;
import com.tcd.asc.damn.common.entity.DublinBikeStation;
import com.tcd.asc.damn.common.entity.Stop;
import com.tcd.asc.damn.common.model.dto.*;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.common.repository.DublinBikeStationRepository;
import com.tcd.asc.damn.common.repository.StopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tcd.asc.damn.common.constants.TransitType;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransitUtil {

    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private DublinBikeStationRepository dublinBikeStationRepository;

    public List<RouteResponse> filterBestRoute(List<RouteResponse> routeResponses) {
        if (routeResponses == null || routeResponses.isEmpty()) {
            return Collections.emptyList();
        }

        RouteResponse bestRoute = null;
        int minStops = Integer.MAX_VALUE;

        for (RouteResponse route : routeResponses) {
            List<RouteSegment> routes = route.getRoutes();

            int totalStops = 0;

            if (routes != null) {
                for (RouteSegment segment : routes) {
                    if (segment instanceof TransitSegment) {
                        TransitSegment transit = (TransitSegment) segment;
                        totalStops += (transit.getStopPath() != null) ? transit.getStopPath().size() : 0;
                        String stopsString = transit.getStopPath().stream()
                                .map(stop -> stop.getStopName() + " (" + stop.getStopId() + ")")
                                .collect(Collectors.joining("->"));
                    }
                }
            }
            if (totalStops < minStops) {
                minStops = totalStops;
                bestRoute = route;
            }
        }
        return bestRoute != null ? Collections.singletonList(bestRoute) : Collections.emptyList();
    }

    public List<Stop> findNearestLuasStops(double lat, double lon, int limit) {
        List<Stop> allStops = stopRepository.findAll().stream()
                .toList();

        return allStops.stream()
                .sorted(Comparator.comparingDouble(s -> haversineDistance(lat, lon, s.getStopLat(), s.getStopLon())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public DublinBikeStation findNearestCycleStop(double lat, double lon, int limit) {
        List<DublinBikeStation> allCycleStops = dublinBikeStationRepository.findAll();

        return allCycleStops.stream()
                .filter(s -> s.getBikes() >= limit)
                .min(Comparator.comparingDouble(s -> haversineDistance(lat, lon, s.getLatitude(), s.getLongitude())))
                .orElse(null);
    }

    public double calculateTravelDistance(List<Coordinates> path) {
        if (path == null || path.size() < 2) return 0.0;
        double distance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Coordinates c1 = path.get(i);
            Coordinates c2 = path.get(i + 1);
            distance += haversineDistance(c1.getLatitude(), c1.getLongitude(), c2.getLatitude(), c2.getLongitude());
        }
        return distance;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public Coordinates findNearestCoordinate(Stop stop, List<Coordinates> coordinates) {
        if (coordinates.isEmpty()) return null;

        double minDistance = Double.MAX_VALUE;
        Coordinates nearest = null;

        for (Coordinates coord : coordinates) {
            double distance = haversineDistance(stop.getStopLat(), stop.getStopLon(),
                    coord.getLatitude(), coord.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = coord;
            }
        }

        if (minDistance > 0.1) {
            //System.out.println("Warning: Nearest coordinate for stop " + stop.getStopName() + " is " + minDistance + " km away, which may be inaccurate.");
            return null;
        }

        return nearest;
    }

    public List<Coordinates> filterTransitPathByStops(List<Stop> stopPath, List<Coordinates> transitPath) {
        if (stopPath.isEmpty() || transitPath.isEmpty()) {
            return new ArrayList<>();
        }

        List<Coordinates> filteredPath = new ArrayList<>();
        Map<Stop, Coordinates> stopToNearestCoord = new HashMap<>();

        for (Stop stop : stopPath) {
            Coordinates nearestCoord = findNearestCoordinate(stop, transitPath);
            if (nearestCoord != null) {
                stopToNearestCoord.put(stop, nearestCoord);
            }
        }

        List<Coordinates> sortedTransitPath = new ArrayList<>(transitPath);
        sortedTransitPath.sort((c1, c2) -> {
            int index1 = transitPath.indexOf(c1);
            int index2 = transitPath.indexOf(c2);
            return Integer.compare(index1, index2);
        });

        List<Integer> stopIndices = stopToNearestCoord.values().stream()
                .map(sortedTransitPath::indexOf)
                .filter(index -> index >= 0)
                .sorted()
                .collect(Collectors.toList());

        if (stopIndices.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = stopIndices.get(0);
        int endIndex = stopIndices.get(stopIndices.size() - 1);
        if (startIndex <= endIndex && endIndex < sortedTransitPath.size()) {
            filteredPath.addAll(sortedTransitPath.subList(startIndex, endIndex + 1));
        }

        return filteredPath;
    }

    public RouteResponse convertToRouteResponse(RouteSegment segment, RouteType routeType, TransitType transitType) {
        RouteResponse routeResponse = new RouteResponse();
        String routeId = UUID.randomUUID().toString();
        routeResponse.setRouteId(routeId);
        routeResponse.setRouteType(routeType);
        routeResponse.setUniqueTransitTypes(Collections.singletonList(transitType));
        routeResponse.setRoutes(Collections.singletonList(segment));
        return routeResponse;
    }
}
