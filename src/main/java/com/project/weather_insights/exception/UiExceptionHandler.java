package com.project.weather_insights.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.project.weather_insights.controller.WeatherDashboardController;

/**
 * HTML errors for the Thymeleaf dashboard only.
 */
@ControllerAdvice(assignableTypes = WeatherDashboardController.class)
public class UiExceptionHandler {

    @ExceptionHandler(UnknownCityException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String unknownCity(UnknownCityException ex, Model model) {
        model.addAttribute("message", "City not found");
        return "error";
    }
}
