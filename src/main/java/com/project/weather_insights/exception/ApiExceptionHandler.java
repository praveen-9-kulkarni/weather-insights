package com.project.weather_insights.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * JSON errors for REST endpoints only ({@link RestController}).
 * HTML controllers are handled by {@link UiExceptionHandler}.
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownCityException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> unknownCity(UnknownCityException ex) {
        return Map.of("error", "City not found");
    }
}
