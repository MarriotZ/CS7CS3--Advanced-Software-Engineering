package com.tcd.asc.damn.common.model.request;

import com.tcd.asc.damn.common.entity.Location;
import lombok.Data;

@Data
public class RouteRequest {
    private Location startLocation;
    private Location endLocation;
}