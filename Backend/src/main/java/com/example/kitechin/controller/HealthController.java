package com.example.kitechin.controller;

import com.example.kitechin.config.MongoDatabaseMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final MongoDatabaseMode databaseMode;

    public HealthController(MongoDatabaseMode databaseMode) {
        this.databaseMode = databaseMode;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("database", databaseMode.mode());
        return body;
    }
}
