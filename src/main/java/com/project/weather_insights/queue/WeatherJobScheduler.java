package com.project.weather_insights.queue;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.weather_insights.service.WeatherService;
@Component
@Profile("!worker")
public class WeatherJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WeatherJobScheduler.class);
    private final WeatherJobQueue weatherJobQueue;

    public WeatherJobScheduler(WeatherJobQueue weatherJobQueue) {
        this.weatherJobQueue = weatherJobQueue;
    }

    @Scheduled(fixedDelay = 300_000)
    public void scheduleJob() {

        List<String> cities = WeatherService.getCities();
        for (String city : cities) {
            Long listSize = weatherJobQueue.enqueue(city);
            logger.info("Enqueued job for city: {} with size: {}", city, listSize);
        }
    }
}
