package com.tcd.asc.damn.common.restclient;

import com.tcd.asc.damn.common.model.request.RouteRequest;
import com.tcd.asc.damn.common.model.response.RouteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "route-provider", url = "http://localhost:8083", path = "/api/routes-provider")
public interface RouteProviderClient {

    @PostMapping("/get-route")
    List<RouteResponse> getRoute(@RequestBody RouteRequest routeRequest);

}