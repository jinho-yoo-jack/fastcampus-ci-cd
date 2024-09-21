package com.fastcampus.cicdstudy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${spring.application.name}")
    String appName;

    @GetMapping
    public String healthCheck(){
        return appName + " Health Statue ::: Good";
    }

}
