package com.project.weather_insights.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.project.weather_insights.model.WeatherReading;
import com.project.weather_insights.repository.WeatherReadingRepository;
import com.project.weather_insights.service.WeatherService.CurrentWeather;

/**
 * Integration-style test: real JPA + H2 + WeatherService.persistIfNewReading.
 * No Open-Meteo, no Redis — only the persist/delta path.
 */
@DataJpaTest
@Import(WeatherService.class)
class WeatherServicePersistTest {

    @Autowired
    WeatherService weatherService;

    @Autowired
    WeatherReadingRepository readings;

    @Test
    void firstReadingHasNullDelta() {
        Instant t1 = Instant.parse("2026-10-01T10:00:00Z");

        weatherService.persistIfNewReading(new CurrentWeather("Bengaluru", 28.0, 0.0, t1));

        WeatherReading saved = readings.findFirstByCityOrderByObservedAtDesc("Bengaluru");
        assertEquals(28.0, saved.getTemperatureCelsius());
        assertNull(saved.getTemperatureDeltaCelsius());
    }

    @Test
    void secondReadingStoresDeltaFromPrevious() {
        Instant t1 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-10-01T11:00:00Z");

        weatherService.persistIfNewReading(new CurrentWeather("Bengaluru", 28.0, 0.0, t1));
        weatherService.persistIfNewReading(new CurrentWeather("Bengaluru", 28.5, 0.0, t2));

        WeatherReading latest = readings.findFirstByCityOrderByObservedAtDesc("Bengaluru");
        assertEquals(28.5, latest.getTemperatureCelsius());
        assertEquals(0.5, latest.getTemperatureDeltaCelsius());
    }

    @Test
    void duplicateObservedAtDoesNotInsertAgain() {
        Instant t1 = Instant.parse("2026-10-01T10:00:00Z");

        weatherService.persistIfNewReading(new CurrentWeather("Mumbai", 30.0, 0.0, t1));
        weatherService.persistIfNewReading(new CurrentWeather("Mumbai", 31.0, 0.0, t1)); // same observedAt

        List<WeatherReading> all = readings.findByCityOrderByObservedAtDesc("Mumbai");
        assertEquals(1, all.size());
        assertEquals(30.0, all.get(0).getTemperatureCelsius());
    }
}
