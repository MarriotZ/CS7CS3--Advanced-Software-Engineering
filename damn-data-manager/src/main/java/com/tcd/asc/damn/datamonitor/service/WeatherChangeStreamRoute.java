package com.tcd.asc.damn.datamonitor.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.tcd.asc.damn.common.model.dto.Coordinates;
import com.tcd.asc.damn.datamonitor.model.WeatherData;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

//@Component
public class WeatherChangeStreamRoute extends RouteBuilder {

//    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "weather:";

    @Override
    public void configure() throws Exception {
        from("mongodb:mongoClient?database=weatherDb&collection=weather&operation=changeStream&fullDocument=updateLookup")
            .process(exchange -> {
                ChangeStreamDocument<?> change = exchange.getIn().getBody(ChangeStreamDocument.class);
                if (change == null || change.getFullDocument() == null) {
                    exchange.getIn().setBody(null);
                    return;
                }

                // Convert MongoDB document to WeatherData
                WeatherData weather = convertToWeatherData((Document) change.getFullDocument());
                if (weather != null && 
                    ("insert".equals(change.getOperationType()) || 
                     "update".equals(change.getOperationType()))) {
                    exchange.getIn().setBody(weather);
                } else {
                    exchange.getIn().setBody(null);
                }
            })
            .choice()
                .when(body().isNotNull())
                    // Update Redis
                    .process(exchange -> {
                        WeatherData weather = exchange.getIn().getBody(WeatherData.class);
                        String cacheKey = CACHE_PREFIX + weather.getId();
                        redisTemplate.opsForValue().set(cacheKey, weather);
                    })
                    // Optional: Trigger an API
                    .marshal().json(JsonLibrary.Jackson)
                    .setHeader("CamelHttpMethod", constant("POST"))
                    .setHeader("Content-Type", constant("application/json"))
                    .to("http://damn-api-gateway:8080/api/weather/notify") // Example endpoint
                    .log("Processed weather change for ${body.id}: ${body}")
                .otherwise()
                    .log("No relevant weather change detected")
            .end();
    }

    private WeatherData convertToWeatherData(org.bson.Document doc) {
        if (doc == null) return null;
        Coordinates coords = new Coordinates(
                doc.getDouble("coordinates.latitude"),
                doc.getDouble("coordinates.longitude")
        );
        return new WeatherData(
                coords,
                doc.getDouble("temperature"),
                doc.getString("condition"),
                doc.getDouble("precipitation"),
                doc.getDouble("windSpeed")
        );
    }
}