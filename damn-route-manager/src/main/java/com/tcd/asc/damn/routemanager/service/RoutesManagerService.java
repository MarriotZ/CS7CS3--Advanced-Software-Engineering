package com.tcd.asc.damn.routemanager.service;

import com.tcd.asc.damn.common.model.request.RouteRequest;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import com.tcd.asc.damn.common.model.response.RoutesResponse;
import com.tcd.asc.damn.common.restclient.RouteProviderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoutesManagerService {

    @Autowired
    private RouteProviderClient routeProviderClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "route:";

    public RoutesResponse getRoutes(RouteRequest routeRequest) {
        // Fetch routes from the external service
        RoutesResponse routesResponse = routeProviderClient.getRoute(routeRequest);

        // Cache each RouteResponse by route_id
        cacheRouteResponses(routesResponse.getRouteResponses());

        return routesResponse;
    }

    private void cacheRouteResponses(List<RouteResponse> routes) {
        if (routes != null) {
            for (RouteResponse route : routes) {
                String cacheKey = CACHE_PREFIX + route.getRouteId();
                // Store in Redis without TTL (persists until manually removed)
                redisTemplate.opsForValue().set(cacheKey, route);
            }
        }
    }
}