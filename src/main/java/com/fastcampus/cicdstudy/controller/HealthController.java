package com.fastcampus.cicdstudy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthController {

    @GetMapping
    public String healthGood(){
        return "Health Good";
    }

    @GetMapping("notbad")
    public String healthNotBad(){
        return "Health Not Bad";

    }

}
