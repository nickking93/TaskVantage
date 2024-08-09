package com.taskvantage.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/api/tasks/test")
    public String testEndpoint() {
        return "TaskVantage API is working!";
    }
}
