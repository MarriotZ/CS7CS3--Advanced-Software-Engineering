package com.tcd.asc.damn.routeprovider.utils;

import com.tcd.asc.damn.common.entity.DublinBikeStation;
import com.tcd.asc.damn.common.model.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CyclingSegmentMapper {

    public static List<CyclingSegment> createCyclingSegments(OsrmRouteResponse osrmResponse, DublinBikeStation startDockStation, DublinBikeStation endDockStation) {
        List<CyclingSegment> cyclingSegments = new ArrayList<>();

        // Check if response is valid
        if (osrmResponse == null || !"Ok".equals(osrmResponse.getCode()) || osrmResponse.getRoutes() == null) {
            return cyclingSegments;
        }

        // Get waypoints for start and end coordinates
        List<OsrmWaypoint> waypoints = osrmResponse.getWaypoints();

        // Process each route
        for (OsrmRoute route : osrmResponse.getRoutes()) {
            CyclingSegment segment = new CyclingSegment();

            // Set coordinates
            segment.setStartDockStation(startDockStation);
            segment.setEndDockStation(endDockStation);

            // Set cyclePath
            List<List<Double>> routeCoordinates = route.getGeometry().getCoordinates();
            List<Coordinates> cyclePath = routeCoordinates.stream()
                    .map(coord -> new Coordinates(coord.get(1), coord.get(0))) // [lon, lat] to (lat, lon)
                    .collect(Collectors.toList());
            segment.setCyclingPath(cyclePath);

            // Set instructions
            List<OsrmStep> steps = route.getLegs().get(0).getSteps();
            List<String> instructions = steps.stream()
                    .map(CyclingSegmentMapper::generateInstruction)
                    .collect(Collectors.toList());
            segment.setInstructions(instructions);

            // Set inherited fields
            segment.setDistance(route.getDistance());
            segment.setDuration(route.getDuration());

            cyclingSegments.add(segment);
        }

        return cyclingSegments;
    }

    private static String generateInstruction(OsrmStep step) {
        String maneuverType = step.getManeuver() != null ? step.getManeuver().getType() : "";
        String modifier = step.getManeuver() != null ? step.getManeuver().getModifier() : "";
        String name = step.getName() != null ? step.getName() : "";
        double distance = step.getDistance();
        String mode = step.getMode() != null ? step.getMode() : "cycling";

        // Determine path type based on mode
        String pathType = "pushing bike".equals(mode) ? "path" : determinePathType(step);

        // Handle special cases
        if ("arrive".equals(maneuverType)) {
            return name.isEmpty() ? "Arrive at your destination" : "Arrive at " + name;
        }
        if ("rotary".equals(maneuverType) || "roundabout turn".equals(maneuverType)) {
            String rotaryName = step.getRotaryName() != null ? step.getRotaryName() : "roundabout";
            return String.format("%s the %s and take the %s exit onto %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Walk through" : "Cycle through",
                    rotaryName, modifier, pathType, name, distance);
        }
        if ("new name".equals(maneuverType)) {
            return String.format("%s on %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Continue walking" : "Continue cycling",
                    pathType, name, distance);
        }
        if ("turn".equals(maneuverType)) {
            return String.format("%s %s onto %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Walk" : "Turn",
                    modifier, pathType, name, distance);
        }
        if ("continue".equals(maneuverType)) {
            return String.format("%s straight on %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Continue walking" : "Continue cycling",
                    pathType, name, distance);
        }
        if ("depart".equals(maneuverType)) {
            return String.format("%s on %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Start walking" : "Start cycling",
                    pathType, name, distance);
        }
        if ("fork".equals(maneuverType)) {
            return String.format("Take the %s fork onto %s %s (%.0f meters)",
                    modifier, pathType, name, distance);
        }
        if ("end of road".equals(maneuverType)) {
            return String.format("%s %s at the end of the %s onto %s %s (%.0f meters)",
                    "pushing bike".equals(mode) ? "Walk" : "Turn",
                    modifier, pathType, pathType, name, distance);
        }

        // Default case
        return String.format("%s %s on %s %s (%.0f meters)",
                "pushing bike".equals(mode) ? capitalize(maneuverType) + " walking" : capitalize(maneuverType) + " cycling",
                modifier, pathType, name, distance);
    }

    private static String determinePathType(OsrmStep step) {
        String name = step.getName() != null ? step.getName().toLowerCase() : "";
        if (name.contains("cycle") || name.contains("bike")) {
            return "cycleway";
        }
        if (name.contains("path") || name.contains("trail")) {
            return "path";
        }
        return "road";
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}