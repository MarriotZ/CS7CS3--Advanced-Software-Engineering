package com.tcd.asc.damn.damnserviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DamnServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DamnServiceRegistryApplication.class, args);
    }

}
