package com.tcd.asc.damn.common.model.response;

import com.tcd.asc.damn.common.model.dto.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoutesResponse implements Serializable {
    private Coordinates startLocation;
    private Coordinates endLocation;
    private Integer noOfRoutes;
    private List<RouteResponse> routeResponses;
}
