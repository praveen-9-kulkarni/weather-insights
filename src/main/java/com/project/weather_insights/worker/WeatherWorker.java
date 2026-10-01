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

            log.info("Picked up job for city={}", city);
            try {
                weatherService.getCurrentWeather();
                log.info("Job for city={} completed", city);
            } catch (Exception e) {
                log.error("Job for city={} failed", city, e);
            }
        }
    }
}
