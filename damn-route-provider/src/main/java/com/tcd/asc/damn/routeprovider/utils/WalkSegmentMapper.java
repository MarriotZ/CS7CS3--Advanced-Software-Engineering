package com.tcd.asc.damn.routeprovider.utils;

import com.tcd.asc.damn.common.model.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WalkSegmentMapper {

    public static List<WalkSegment> createWalkSegments(OsrmRouteResponse osrmResponse) {
        List<WalkSegment> walkSegments = new ArrayList<>();

        // Check if response is valid
        if (osrmResponse == null || !"Ok".equals(osrmResponse.getCode()) || osrmResponse.getRoutes() == null) {
            return walkSegments;
        }

        // Get waypoints for start and end coordinates
        List<OsrmWaypoint> waypoints = osrmResponse.getWaypoints();
        Coordinates startCoordinate = waypoints.size() > 0
                ? new Coordinates(waypoints.get(0).getLocation().get(1), waypoints.get(0).getLocation().get(0))
                : null;
        Coordinates endCoordinate = waypoints.size() > 1
                ? new Coordinates(waypoints.get(1).getLocation().get(1), waypoints.get(1).getLocation().get(0))
                : null;

        // Process each route
        for (OsrmRoute route : osrmResponse.getRoutes()) {
            WalkSegment segment = new WalkSegment();

            // Set coordinates
            segment.setStartCoordinate(startCoordinate);
            segment.setEndCoordinate(endCoordinate);

            // Set walkPath
            List<List<Double>> routeCoordinates = route.getGeometry().getCoordinates();
            List<Coordinates> walkPath = routeCoordinates.stream()
                    .map(coord -> new Coordinates(coord.get(1), coord.get(0))) // [lon, lat] to (lat, lon)
                    .collect(Collectors.toList());
            segment.setWalkPath(walkPath);

            // Set instructions
            List<OsrmStep> steps = route.getLegs().get(0).getSteps();
            List<String> instructions = steps.stream()
                    .map(WalkSegmentMapper::generateInstruction)
                    .collect(Collectors.toList());
            segment.setInstructions(instructions);

            // Set inherited fields
            segment.setDistance(route.getDistance());
            segment.setDuration(route.getDuration());

            walkSegments.add(segment);
        }

        return walkSegments;
    }

    private static String generateInstruction(OsrmStep step) {
        String maneuverType = step.getManeuver() != null ? step.getManeuver().getType() : "";
        String modifier = step.getManeuver() != null ? step.getManeuver().getModifier() : "";
        String name = step.getName() != null ? step.getName() : "";
        double distance = step.getDistance();
        String pathType = determinePathType(step);

        // Handle special cases
        if ("arrive".equals(maneuverType)) {
            return name.isEmpty() ? "Arrive at your destination" : "Arrive at " + name;
        }
        if ("rotary".equals(maneuverType) || "roundabout turn".equals(maneuverType)) {
            String rotaryName = step.getRotaryName() != null ? step.getRotaryName() : "roundabout";
            return String.format("Enter the %s and take the %s exit onto %s %s (%.0f meters)",
                    rotaryName, modifier, pathType, name, distance);
        }
        if ("new name".equals(maneuverType)) {
            return String.format("Continue walking on %s %s (%.0f meters)", pathType, name, distance);
        }
        if ("turn".equals(maneuverType)) {
            return String.format("Turn %s onto %s %s (%.0f meters)", modifier, pathType, name, distance);
        }
        if ("continue".equals(maneuverType)) {
            return String.format("Walk straight on %s %s (%.0f meters)", pathType, name, distance);
        }
        if ("depart".equals(maneuverType)) {
            return String.format("Start walking on %s %s (%.0f meters)", pathType, name, distance);
        }
        if ("fork".equals(maneuverType)) {
            return String.format("Take the %s fork onto %s %s (%.0f meters)", modifier, pathType, name, distance);
        }
        if ("end of road".equals(maneuverType)) {
            return String.format("Turn %s at the end of the %s onto %s %s (%.0f meters)",
                    modifier, pathType, pathType, name, distance);
        }

        // Default case
        return String.format("%s %s on %s %s (%.0f meters)",
                capitalize(maneuverType), modifier, pathType, name, distance);
    }

    private static String determinePathType(OsrmStep step) {
        String name = step.getName() != null ? step.getName().toLowerCase() : "";
        String mode = step.getMode() != null ? step.getMode() : "walking";

        if ("walking".equals(mode)) {
            if (name.contains("path") || name.contains("walk") || name.contains("pedestrian")) {
                return "path";
            }
            if (name.contains("bridge")) {
                return "bridge";
            }
            if (name.contains("quay") || name.contains("street") || name.contains("lane")) {
                return "street";
            }
        }
        return "path"; // Default for walking mode
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}