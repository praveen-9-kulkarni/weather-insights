package com.project.weather_insights.exception;

public class UnknownCityException extends RuntimeException {

    public UnknownCityException(String city) {
        super("City " + city + " not found in our catalog!!!");
    }
}
