package com.project.weather_insights.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.project.weather_insights.model.WeatherReading;
import com.project.weather_insights.queue.WeatherJobQueue;
import com.project.weather_insights.service.WeatherService;
import com.project.weather_insights.service.WeatherService.CurrentWeather;

@RestController
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherJobQueue weatherJobQueue;
    private final byte[] adminTokenBytes;

    public WeatherController(
            WeatherService weatherService,
            WeatherJobQueue weatherJobQueue,
            @Value("${weather.admin.token}") String adminToken) {
        this.weatherService = weatherService;
        this.weatherJobQueue = weatherJobQueue;
        this.adminTokenBytes = adminToken.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/weather")
    public CurrentWeather getCurrentWeather(@RequestParam String city) {

        return weatherService.getLatestReading(city);
    }

    @GetMapping("/weather/readings")
    public List<WeatherReading> listReadings(@RequestParam String city) {

        return weatherService.listReadings(city);
    }

    @PostMapping("/weather/jobs")
    public Map<String, String> scheduleJob(@RequestParam String city) {
        Long listSize = weatherJobQueue.enqueue(city);
        return Map.of("status", "enqueued", "size", listSize.toString());
    }

    @PostMapping("/weather/jobs/replay")
    public Map<String, String> replayJobs(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdminToken(token);

        int count = weatherJobQueue.replayAllDeadLetterQueueJobs();
        if (count == 0) {
            return Map.of("status", "empty");
        }
        return Map.of("status", "replayed", "count", Integer.toString(count));
    }

    private void requireAdminToken(String token) {
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        byte[] provided = token.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(adminTokenBytes, provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
