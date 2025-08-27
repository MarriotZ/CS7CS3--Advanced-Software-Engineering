package com.tcd.asc.damn.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tcd.asc.damn.common.constants.TransitType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "transitType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WalkSegment.class, name = "WALK"),
        @JsonSubTypes.Type(value = TransitSegment.class, name = "LUAS"),
        @JsonSubTypes.Type(value = DriveSegment.class, name = "DRIVING"),
        @JsonSubTypes.Type(value = CyclingSegment.class, name = "CYCLING")
})
public abstract class RouteSegment {
    @JsonIgnore
    protected TransitType transitType;
}