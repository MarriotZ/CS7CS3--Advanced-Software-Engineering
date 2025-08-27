package com.tcd.asc.damn.common.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcd.asc.damn.common.constants.TransitType;
import com.tcd.asc.damn.common.entity.DublinBikeStation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CyclingSegment extends RouteSegment {
    private DublinBikeStation startDockStation;
    private DublinBikeStation endDockStation;
    @JsonProperty("cyclingPath")
    private List<Coordinates> cyclingPath;
    private List<String> instructions;
    protected Double distance;
    protected Double duration;

    public CyclingSegment() {
        super(TransitType.CYCLING);
    }
}