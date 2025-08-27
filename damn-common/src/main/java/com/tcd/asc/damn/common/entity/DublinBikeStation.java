package com.tcd.asc.damn.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DublinBikeStation {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "idx", nullable = false)
    private Integer idx;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "latitude", nullable = false)
    //@JsonProperty("lat")
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    //@JsonProperty("lng")
    private Double longitude;

    @Column(name = "timestamp", nullable = false)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;

    @Column(name = "bikes", nullable = false)
    private Integer bikes;

    @Column(name = "free", nullable = false)
    private Integer free;
}