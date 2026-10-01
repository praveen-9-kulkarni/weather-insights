package com.project.weather_insights.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.weather_insights.service.WeatherService;

@Controller
public class WeatherDashboardController {

    private final WeatherService weatherService;

    public WeatherDashboardController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("readings", weatherService.listLatestReadings());
        return "dashboard";
    }

    @GetMapping("/cities/{city}")
    public String cityReadings(Model model, @PathVariable String city) {

        model.addAttribute("cityName", city);
        model.addAttribute("readings", weatherService.listReadings(city));
        return "city";
    }
}
