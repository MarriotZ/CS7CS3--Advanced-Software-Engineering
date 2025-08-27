package com.tcd.asc.damn.common.model.request;

import com.tcd.asc.damn.common.constants.RouteType;
import com.tcd.asc.damn.common.constants.DistanceUnit;
import lombok.Data;

@Data
public class UserPreferences {
    private boolean avoidTolls = false;
    private boolean avoidHighways = false;
    private boolean avoidFerries = false;
    private RouteType preferredMode = RouteType.DRIVING;
    private DistanceUnit distanceUnit = DistanceUnit.KILOMETERS;
    private boolean wheelchairAccessible = false;
    private boolean avoidStairs = false;
    private boolean showAllModes = false;
}
