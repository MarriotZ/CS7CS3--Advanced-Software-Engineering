package com.tcd.asc.damn.routeprovider.utils;

import com.tcd.asc.damn.common.model.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DriveSegmentMapper {

    public static List<DriveSegment> createDriveSegments(OsrmRouteResponse osrmResponse) {
        List<DriveSegment> driveSegments = new ArrayList<>();

        // Check if response is valid
        if (osrmResponse == null || !"Ok".equals(osrmResponse.getCode()) || osrmResponse.getRoutes() == null) {
            return driveSegments;
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
            DriveSegment segment = new DriveSegment();

            // Set coordinates
            segment.setStartCoordinate(startCoordinate);
            segment.setEndCoordinate(endCoordinate);

            // Set drivePath
            List<List<Double>> routeCoordinates = route.getGeometry().getCoordinates();
            List<Coordinates> drivePath = routeCoordinates.stream()
                    .map(coord -> new Coordinates(coord.get(1), coord.get(0))) // [lon, lat] to (lat, lon)
                    .collect(Collectors.toList());
            segment.setDrivePath(drivePath);

            // Set instructions
            List<OsrmStep> steps = route.getLegs().get(0).getSteps();
            List<String> instructions = steps.stream()
                    .map(step -> generateInstruction(step))
                    .collect(Collectors.toList());
            segment.setInstructions(instructions);

            // Set inherited fields (assuming RouteSegment has these)
            segment.setDistance(route.getDistance());
            segment.setDuration(route.getDuration());

            driveSegments.add(segment);
        }

        return driveSegments;
    }

    private static String generateInstruction(OsrmStep step) {
        String maneuverType = step.getManeuver() != null ? step.getManeuver().getType() : "";
        String modifier = step.getManeuver() != null ? step.getManeuver().getModifier() : "";
        String name = step.getName() != null ? step.getName() : "";
        double distance = step.getDistance();

        // Handle special cases
        if ("arrive".equals(maneuverType)) {
            return "Arrive at " + name;
        }
        if ("rotary".equals(maneuverType) || "roundabout turn".equals(maneuverType)) {
            String rotaryName = step.getRotaryName() != null ? step.getRotaryName() : "roundabout";
            return String.format("Enter %s and take the %s exit onto %s (%.1f meters)",
                    rotaryName, modifier, name, distance);
        }
        if ("new name".equals(maneuverType)) {
            return String.format("Continue on %s (%.1f meters)", name, distance);
        }
        if ("turn".equals(maneuverType) || "continue".equals(maneuverType)) {
            return String.format("Turn %s onto %s (%.1f meters)", modifier, name, distance);
        }

        // Default case
        return String.format("%s %s on %s (%.1f meters)",
                capitalize(maneuverType), modifier, name, distance);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}