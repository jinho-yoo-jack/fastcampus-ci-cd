package com.fastcampus.cicdstudy.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthController {

    public String healthGood(){
        return "Health Good";
    }

}
