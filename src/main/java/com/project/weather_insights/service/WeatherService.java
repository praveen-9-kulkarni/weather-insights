package com.project.weather_insights.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.project.weather_insights.model.WeatherReading;
import com.project.weather_insights.repository.WeatherReadingRepository;

@Service
public class WeatherService {

    private static final String CITY = "Bengaluru";
    private static final double LATITUDE = 12.97;
    private static final double LONGITUDE = 77.59;
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private final WeatherReadingRepository readings;
    private RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();
    record Current(LocalDateTime time, double temperature_2m, double precipitation) {}
    record ParentResponse(Current current) {}
    public record CurrentWeather(String city, double temperatureCelsius, double precipitationMillimetres, Instant observedAt) {}

    public WeatherService(WeatherReadingRepository readings) {
        this.readings = readings;
    }
    
    public CurrentWeather getCurrentWeather() {
        
        ParentResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder.queryParam("latitude", LATITUDE).queryParam("longitude", LONGITUDE).queryParam("current", "temperature_2m,precipitation").build())
                        .retrieve()
                        .body(ParentResponse.class);

        CurrentWeather weather = new CurrentWeather(CITY, response.current().temperature_2m(), response.current().precipitation(), response.current().time().atZone(ZoneId.of("GMT")).toInstant());

        if (!readings.existsByCityAndObservedAt(weather.city(), weather.observedAt())) {
            readings.save(
                new WeatherReading(
                    weather.city(),
                    weather.temperatureCelsius(),
                    weather.precipitationMillimetres(),
                    weather.observedAt()
                )
            );
        }

        return weather;
    }

    public CurrentWeather getLatestReading() {

        WeatherReading reading = readings.findFirstByCityOrderByObservedAtDesc(CITY);
        if (reading == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No readings found for city " + CITY);
        }
        return new CurrentWeather(reading.getCity(), reading.getTemperatureCelsius(), reading.getPrecipitationMillimetres(), reading.getObservedAt());
    }

    public List<WeatherReading> listReadings() {
        
        return readings.findByCityOrderByObservedAtDesc(CITY);
    }
}
