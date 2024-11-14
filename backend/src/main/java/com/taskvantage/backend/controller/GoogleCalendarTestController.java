package com.taskvantage.backend.controller;

import com.taskvantage.backend.service.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Profile("dev")  // Only active in the "dev" profile
public class GoogleCalendarTestController {

    private final GoogleCalendarService googleCalendarService;

    @Autowired
    public GoogleCalendarTestController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @GetMapping("/google-calendar")
    public String testGoogleCalendar() {
        try {
            googleCalendarService.testGoogleCalendarIntegration();
            return "Google Calendar event created!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to create Google Calendar event: " + e.getMessage();
        }
    }
}