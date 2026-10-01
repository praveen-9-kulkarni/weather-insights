package com.project.weather_insights.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.weather_insights.model.WeatherReading;

public interface WeatherReadingRepository extends JpaRepository<WeatherReading, Long> {

    boolean existsByCityAndObservedAt(String city, Instant observedAt);

    List<WeatherReading> findByCityOrderByObservedAtDesc(String city);

    WeatherReading findFirstByCityOrderByObservedAtDesc(String city);
}