package com.project.weather_insights.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!worker")
public class WeatherJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WeatherJobScheduler.class);
    private final WeatherJobQueue weatherJobQueue;
    private static final String CITY = "Bengaluru";

    public WeatherJobScheduler(WeatherJobQueue weatherJobQueue) {
        this.weatherJobQueue = weatherJobQueue;
    }

    @Scheduled(fixedDelay = 60_000)
    public void scheduleJob() {
        Long listSize = weatherJobQueue.enqueue(CITY);
        logger.info("Enqueued job for city: {} with size: {}", CITY, listSize);
    }
}
