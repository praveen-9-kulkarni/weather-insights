package com.project.weather_insights.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "weather_reading",
        uniqueConstraints = @UniqueConstraint(columnNames = {"city", "observed_at"})
)
public class WeatherReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private double temperatureCelsius;

    @Column(nullable = false)
    private double precipitationMillimetres;

    @Column(nullable = false)
    private Instant observedAt;

    protected WeatherReading() {
    }

    public WeatherReading(String city, double temperatureCelsius, double precipitationMillimetres, Instant observedAt) {
        this.city = city;
        this.temperatureCelsius = temperatureCelsius;
        this.precipitationMillimetres = precipitationMillimetres;
        this.observedAt = observedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getPrecipitationMillimetres() {
        return precipitationMillimetres;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}