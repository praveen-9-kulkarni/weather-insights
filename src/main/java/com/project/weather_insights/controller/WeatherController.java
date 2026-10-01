package com.project.weather_insights.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.weather_insights.model.WeatherReading;
import com.project.weather_insights.queue.WeatherJobQueue;
import com.project.weather_insights.service.WeatherService;
import com.project.weather_insights.service.WeatherService.CurrentWeather;

@RestController
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherJobQueue weatherJobQueue;
    
    public WeatherController(WeatherService weatherService, WeatherJobQueue weatherJobQueue) {
        this.weatherService = weatherService;
        this.weatherJobQueue = weatherJobQueue;
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
}
