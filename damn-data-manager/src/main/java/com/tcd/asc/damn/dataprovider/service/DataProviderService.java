package com.tcd.asc.damn.dataprovider.service;

import com.tcd.asc.damn.common.constants.StationType;
import com.tcd.asc.damn.common.entity.Location;
import com.tcd.asc.damn.common.entity.Route;
import com.tcd.asc.damn.common.entity.Station;
import com.tcd.asc.damn.common.repository.RouteRepository;
import com.tcd.asc.damn.common.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DataProviderService {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteRepository routeRepository;

    public List<Station> getAllStations(StationType stationType) {
        return stationRepository.findByType(stationType);
    }

    public Station findNearestStation(Location location) {
        Station nearestStation = null;
        double shortestDistance = 100;

        for (Station station : getAllStations(StationType.LUAS)) {
            Location stationLocation = station.getLocation();
            double distance = calculateDistance(stationLocation.getLatitude(), stationLocation.getLongitude(), location.getLatitude(), location.getLongitude());
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestStation = station;
            }
        }

        return nearestStation;
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    public List<String> getStationNamesBetween(Long endStationId, Long startStationId) {
        // Fetch all routes
        List<Route> allRoutes =  routeRepository.findAll();

        // Create a list to hold station names
        List<String> stationNames = new ArrayList<>();
        int noOfStations = 0;
        double totalDistance = 0;

        // Start the traversal
        Long currentStationId = startStationId;
        while (currentStationId != null) {
            // Find the next route that starts with the current station
            Long finalCurrentStationId = currentStationId;
            Route nextRoute = allRoutes.stream()
                    .filter(tempRoute -> tempRoute.getStartStation().getStationId().equals(finalCurrentStationId))
                    .findFirst()
                    .orElse(null);

            // Break if no further route is found
            if (nextRoute == null) {
                break;
            }

            // Add the start station name to the list
            stationNames.add(nextRoute.getStartStation().getName());
            noOfStations++;
            totalDistance+=nextRoute.getDistance();

            // Check if we have reached the end station
            if (nextRoute.getEndStation().getStationId().equals(endStationId)) {
                stationNames.add(nextRoute.getEndStation().getName());
                noOfStations++;
                totalDistance+=nextRoute.getDistance();
                break;
            }

            // Move to the next station
            currentStationId = nextRoute.getEndStation().getStationId();
        }
        Collections.reverse(stationNames);
//        route.setStationsList(stationNames);
//        route.setTravelCost(noOfStations * 0.5);
//        route.setTravelMode(TravelMode.LUAS);
//        route.setTravelDistance(totalDistance);
        return stationNames;
    }
}
