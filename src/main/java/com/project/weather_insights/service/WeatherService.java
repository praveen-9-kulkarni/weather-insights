package com.project.weather_insights.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.project.weather_insights.exception.UnknownCityException;
import com.project.weather_insights.model.WeatherReading;
import com.project.weather_insights.repository.WeatherReadingRepository;

@Service
public class WeatherService {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private final WeatherReadingRepository readings;
    private RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();
    record Current(LocalDateTime time, double temperature_2m, double precipitation) {}
    record ParentResponse(Current current) {}
    public record CurrentWeather(String city, double temperatureCelsius, double precipitationMillimetres, Instant observedAt) {}
    public record City(String name, double latitude, double longitude){};
    private static final Map<String, City> CITIES = Map.of(
        "Bengaluru", new City("Bengaluru", 12.97, 77.59),
        "Mumbai", new City("Mumbai", 19.07, 72.87),
        "Delhi", new City("Delhi", 28.66, 77.21),
        "Chennai", new City("Chennai", 13.08, 80.27),
        "Kolkata", new City("Kolkata", 22.57, 88.36),
        "Hyderabad", new City("Hyderabad", 17.38, 78.47)
    );

    public WeatherService(WeatherReadingRepository readings) {
        this.readings = readings;
    }
    
    public CurrentWeather getCurrentWeather(String city) {
        
        City cityData = resolveCityOrThrow(city);
        ParentResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder.queryParam("latitude", cityData.latitude()).queryParam("longitude", cityData.longitude()).queryParam("current", "temperature_2m,precipitation").build())
                        .retrieve()
                        .body(ParentResponse.class);

        CurrentWeather weather = new CurrentWeather(cityData.name(), response.current().temperature_2m(), response.current().precipitation(), response.current().time().atZone(ZoneId.of("GMT")).toInstant());

        WeatherReading latestReading = readings.findFirstByCityOrderByObservedAtDesc(cityData.name());

        if (latestReading == null) {
            readings.save(
                new WeatherReading(
                    weather.city(),
                    weather.temperatureCelsius(),
                    null,
                    weather.precipitationMillimetres(),
                    weather.observedAt()
                )
            );
        } else {
            if (!latestReading.getObservedAt().equals(weather.observedAt())) {
                readings.save(
                    new WeatherReading(
                        weather.city(),
                        weather.temperatureCelsius(),
                        weather.temperatureCelsius() - latestReading.getTemperatureCelsius(),
                        weather.precipitationMillimetres(),
                        weather.observedAt()
                    )
                );
            }
        }

        return weather;
    }

    public static City resolveCityOrThrow(String city) {

        City cityData = CITIES.get(city);
        if (cityData == null) {
            throw new UnknownCityException(city);
        }
        return cityData;
    }

    public CurrentWeather getLatestReading(String city) {

        City cityData = resolveCityOrThrow(city);
        WeatherReading reading = readings.findFirstByCityOrderByObservedAtDesc(cityData.name());
        if (reading == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No readings found for city " + city);
        }
        return new CurrentWeather(reading.getCity(), reading.getTemperatureCelsius(), reading.getPrecipitationMillimetres(), reading.getObservedAt());
    }

    public List<WeatherReading> listReadings(String city) {
        
        City cityData = resolveCityOrThrow(city);
        return readings.findByCityOrderByObservedAtDesc(cityData.name());
    }

    public static List<String> getCities() {
    
        return CITIES.keySet().stream().toList();
    }
}
