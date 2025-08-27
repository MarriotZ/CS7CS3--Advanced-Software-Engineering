package com.tcd.asc.damn.routeprovider.service;

import com.tcd.asc.damn.common.constants.TransitType;
import com.tcd.asc.damn.common.entity.Shape;
import com.tcd.asc.damn.common.entity.Stop;
import com.tcd.asc.damn.common.entity.Trip;
import com.tcd.asc.damn.common.model.dto.Coordinates;
import com.tcd.asc.damn.common.model.dto.RouteSegment;
import com.tcd.asc.damn.common.model.dto.TransitSegment;
import com.tcd.asc.damn.common.model.dto.WalkSegment;
import com.tcd.asc.damn.common.repository.ShapeRepository;
import com.tcd.asc.damn.common.repository.StopRepository;
import com.tcd.asc.damn.common.repository.TripRepository;
import com.tcd.asc.damn.routeprovider.utils.TransitUtil;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class Neo4jService {

    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private TransitUtil transitUtil;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ShapeRepository shapeRepository;
    @Autowired
    private OsrmService osrmService;

    private static final double TRANSFER_PENALTY = 5.0;

    public List<RouteSegment> findPubTransitRouteBetweenStops(Stop startStop, Stop endStop, Session session, StringBuilder errorDetails) {
        // Use apoc.algo.aStar to find the shortest path with RED_LUAS, TRANSFER, or GREEN_LUAS relationships
        Result result = session.run(
                "MATCH (start:Stop {stopId: $startStopId}), (end:Stop {stopId: $endStopId}) " +
                        "CALL apoc.algo.aStar(" +
                        "  start, " +
                        "  end, " +
                        "  'RED_LUAS>|GREEN_LUAS>|TRANSFER>', " +
                        "  'weight', " +
                        "  'stopLat', " +
                        "  'stopLon'" +
                        ") YIELD path, weight " +
                        "RETURN [node IN nodes(path) | {stopId: node.stopId, stopName: node.stopName, " +
                        "stopLat: node.stopLat, stopLon: node.stopLon}] AS stopPath, " +
                        "relationships(path) AS rawRelationships, " +
                        "weight AS totalWeight",
                Map.of("startStopId", startStop.getStopId(), "endStopId", endStop.getStopId())
        );

        if (!result.hasNext()) {
            String debugInfo = String.format(
                    "No path found between %s (ID: %s) and %s (ID: %s). " +
                            "Possible reasons: Graph might be disconnected or search depth exceeded.",
                    startStop.getStopName(), startStop.getStopId(),
                    endStop.getStopName(), endStop.getStopId()
            );
            return Collections.emptyList();
        }

        var record = result.single();
        List<Object> rawStopPath = record.get("stopPath").asList();
        List<Value> rawRelationships = record.get("rawRelationships").asList(Values::value);
        double totalWeight = record.get("totalWeight").asDouble();

        // Convert raw path to Stop objects
        List<Stop> stopPath = rawStopPath.stream()
                .map(obj -> {
                    Map<String, Object> node = (Map<String, Object>) obj;
                    return stopRepository.findById((String) node.get("stopId")).get();
                })
                .collect(Collectors.toList());

        // Verify no loops
        Set<String> visitedStopIds = new HashSet<>();
        for (Stop stop : stopPath) {
            if (!visitedStopIds.add(stop.getStopId())) {
                System.out.println("Loop detected in route at " + stop.getStopName() + " (ID: " + stop.getStopId() + "). Path discarded.");
                return null;
            }
        }

        // Process relationships to determine segments
        List<RouteSegment> segments = new ArrayList<>();
        List<String> relationshipTypes = rawRelationships.stream()
                .map(rel -> rel.asRelationship().type())
                .collect(Collectors.toList());
        List<String> allTripIds = new ArrayList<>();
        double currentWeight = 0.0;

        for (Value relValue : rawRelationships) {
            Map<String, Object> relProps = relValue.asMap();
            double weight = relProps.containsKey("weight") ? ((Number) relProps.get("weight")).doubleValue() : 1.0;
            currentWeight += weight;
            if (relProps.containsKey("tripIds")) {
                Object tripIdsObj = relProps.get("tripIds");
                if (tripIdsObj instanceof List) {
                    allTripIds.addAll((List<String>) tripIdsObj);
                } else if (tripIdsObj instanceof String) {
                    allTripIds.add((String) tripIdsObj);
                }
            }
        }

        // If there are no TRANSFER relationships, create a single TransitSegment
        if (!relationshipTypes.contains("TRANSFER")) {
            List<Coordinates> transitPath = new ArrayList<>();
            allTripIds = allTripIds.stream().distinct().collect(Collectors.toList());
            if (!allTripIds.isEmpty()) {
                String selectedTripId = allTripIds.get(0); // Use the first trip ID
                Trip trip = tripRepository.findById(selectedTripId)
                        .orElseThrow(() -> new RuntimeException("Trip not found: " + selectedTripId));
                String shapeId = trip.getShapeId();
                if (shapeId != null) {
                    List<Shape> shapes = shapeRepository.findByShapeId(shapeId);
                    transitPath = shapes.stream()
                            .sorted(Comparator.comparingInt(Shape::getShapePtSequence))
                            .map(shape -> new Coordinates(shape.getShapePtLat(), shape.getShapePtLon()))
                            .collect(Collectors.toList());
                } else {
                    System.out.println("No shapeId found for trip: " + selectedTripId);
                }
            }

            List<Coordinates> filteredTransitPath = transitUtil.filterTransitPathByStops(stopPath, transitPath);
            TransitSegment transitSegment = new TransitSegment();
            transitSegment.setBoardingStop(startStop);
            transitSegment.setAlightingStop(endStop);
            transitSegment.setStopPath(stopPath);
            transitSegment.setTransitPath(filteredTransitPath);
            transitSegment.setTransitType(TransitType.LUAS);
            double travelDistance = transitUtil.calculateTravelDistance(filteredTransitPath);
            transitSegment.setTravelDistance(travelDistance);
            double travelTime = currentWeight * 60; // Convert to seconds
            transitSegment.setTravelTime(travelTime);
            transitSegment.setTravelCost(travelDistance * 0.1);

            segments.add(transitSegment);
        } else {
            // Handle transfers
            List<Integer> transferIndices = new ArrayList<>();
            for (int i = 0; i < relationshipTypes.size(); i++) {
                if (relationshipTypes.get(i).equals("TRANSFER")) {
                    transferIndices.add(i);
                }
            }

            int startIndex = 0;
            List<String> segmentTripIds = new ArrayList<>();
            double segmentWeight = 0.0;

            for (int i = 0; i <= transferIndices.size(); i++) {
                int endIndex = (i < transferIndices.size()) ? transferIndices.get(i) + 1 : stopPath.size();
                List<Stop> segmentStops = stopPath.subList(startIndex, endIndex);
                if (segmentStops.size() < 2) {
                    startIndex = endIndex;
                    continue;
                }

                Stop segmentStart = segmentStops.get(0);
                Stop segmentEnd = segmentStops.get(segmentStops.size() - 1);

                // Collect trip IDs for this segment
                segmentTripIds.clear();
                segmentWeight = 0.0;
                for (int j = startIndex; j < endIndex - 1; j++) {
                    Value relValue = rawRelationships.get(j);
                    Map<String, Object> relProps = relValue.asMap();
                    segmentWeight += relProps.containsKey("weight") ? ((Number) relProps.get("weight")).doubleValue() : 1.0;
                    if (relProps.containsKey("tripIds")) {
                        Object tripIdsObj = relProps.get("tripIds");
                        if (tripIdsObj instanceof List) {
                            segmentTripIds.addAll((List<String>) tripIdsObj);
                        } else if (tripIdsObj instanceof String) {
                            segmentTripIds.add((String) tripIdsObj);
                        }
                    }
                }

                List<Coordinates> transitPath = new ArrayList<>();
                segmentTripIds = segmentTripIds.stream().distinct().collect(Collectors.toList());
                if (!segmentTripIds.isEmpty()) {
                    String selectedTripId = segmentTripIds.get(0);
                    Trip trip = tripRepository.findById(selectedTripId)
                            .orElseThrow(() -> new RuntimeException("Trip not found: " + selectedTripId));
                    String shapeId = trip.getShapeId();
                    if (shapeId != null) {
                        List<Shape> shapes = shapeRepository.findByShapeId(shapeId);
                        transitPath = shapes.stream()
                                .sorted(Comparator.comparingInt(Shape::getShapePtSequence))
                                .map(shape -> new Coordinates(shape.getShapePtLat(), shape.getShapePtLon()))
                                .collect(Collectors.toList());
                    }
                }

                List<Coordinates> filteredTransitPath = transitUtil.filterTransitPathByStops(segmentStops, transitPath);
                TransitSegment transitSegment = new TransitSegment();
                transitSegment.setBoardingStop(segmentStart);
                transitSegment.setAlightingStop(segmentEnd);
                transitSegment.setStopPath(segmentStops);
                transitSegment.setTransitPath(filteredTransitPath);
                transitSegment.setTransitType(TransitType.LUAS);
                double travelDistance = transitUtil.calculateTravelDistance(filteredTransitPath);
                transitSegment.setTravelDistance(travelDistance);
                double travelTime = segmentWeight * 60;
                transitSegment.setTravelTime(travelTime);
                transitSegment.setTravelCost(travelDistance * 0.1);

                segments.add(transitSegment);

                // Add WalkSegment for transfer
                if (i < transferIndices.size()) {
                    Coordinates fromCoord = new Coordinates(segmentEnd.getStopLat(), segmentEnd.getStopLon());
                    Stop nextSegmentStart = stopPath.get(endIndex);
                    Coordinates toCoord = new Coordinates(nextSegmentStart.getStopLat(), nextSegmentStart.getStopLon());
                    WalkSegment walkSegment = new WalkSegment();
                    walkSegment.setStartCoordinate(fromCoord);
                    walkSegment.setEndCoordinate(toCoord);
                    walkSegment.setWalkPath(osrmService.getWalkPath(fromCoord, toCoord));
                    segments.add(walkSegment);
                }

                startIndex = endIndex;
            }

            // Apply transfer penalty
            totalWeight += transferIndices.size() * TRANSFER_PENALTY;
        }

        String stopsString = stopPath.stream()
                .map(stop -> stop.getStopName() + " (" + stop.getStopId() + ")")
                .collect(Collectors.joining("->"));
        System.out.println("Route found: " + stopsString + " with " + stopPath.size() + " stops, total weight: " + totalWeight);

        return segments;
    }
}
