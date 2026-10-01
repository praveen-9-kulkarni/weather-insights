CREATE TABLE weather_reading (
    id                              BIGSERIAL PRIMARY KEY,
    city                            VARCHAR(255) NOT NULL,
    temperature_celsius             DOUBLE PRECISION NOT NULL,
    precipitation_millimetres       DOUBLE PRECISION NOT NULL,
    temperature_delta_celsius       DOUBLE PRECISION,
    observed_at                     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_weather_reading_city_observed_at UNIQUE (city, observed_at)
);