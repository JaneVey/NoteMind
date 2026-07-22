package com.notemind.controller;

import com.notemind.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "version", "0.0.1-SNAPSHOT"
        ));
    }
}
