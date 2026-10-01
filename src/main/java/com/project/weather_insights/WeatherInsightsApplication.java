package com.project.weather_insights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeatherInsightsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherInsightsApplication.class, args);
	}

}
