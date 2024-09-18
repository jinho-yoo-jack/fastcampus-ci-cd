package com.fastcampus.cicdstudy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String healthGood(){
        return "Health Good";
    }

    @GetMapping("/bed")
    public String healthBed(){
        return "Health Bed";
    }

    @GetMapping("/not-bed")
    public String healthNotBed(){
        return "Health Not Bed";
    }

}
