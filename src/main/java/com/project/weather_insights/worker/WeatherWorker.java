package com.project.weather_insights.worker;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.project.weather_insights.queue.WeatherJobQueue;
import com.project.weather_insights.service.WeatherService;

/**
 * Runs only when this process is started with the "worker" profile.
 * Blocks on the Redis queue and processes one job at a time, forever.
 */
@Component
@Profile("worker")
public class WeatherWorker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WeatherWorker.class);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 3;

    private final WeatherJobQueue queue;
    private final WeatherService weatherService;

    public WeatherWorker(WeatherJobQueue queue, WeatherService weatherService) {
        this.queue = queue;
        this.weatherService = weatherService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Worker started, waiting for jobs on weather-jobs");

        while (!Thread.currentThread().isInterrupted()) {
            String city = queue.dequeue(POLL_TIMEOUT);
            if (city == null) {
                continue; // timed out, nothing arrived; go back and wait again
            }
            String[] cityParts = city.split(":");
            int attemptCount = cityParts.length > 1 ? Integer.parseInt(cityParts[1]) : 1;
            String cityName = cityParts[0];
            log.info("Picked up job for city={}", cityName);
            try {                
                weatherService.getCurrentWeather(cityName);
                log.info("Job for city={} completed", cityName);
            } catch (Exception e) {
                if (attemptCount < MAX_RETRIES) {
                    log.warn("Job for city={} failed (attempt {}), re-queueing", cityName, attemptCount, e);
                    queue.enqueue(cityName, attemptCount + 1);
                } else {
                    log.error("Job for city={} failed", cityName, e);
                    queue.deadLetter(cityName);
                }
            }
        }
    }
}
