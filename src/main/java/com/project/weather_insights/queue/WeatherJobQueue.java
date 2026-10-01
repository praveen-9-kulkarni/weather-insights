package com.project.weather_insights.queue;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WeatherJobQueue {

    private final StringRedisTemplate redis;
    private static final String QUEUE_KEY = "weather-jobs";
    
    public WeatherJobQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Long enqueue(String city) {
        return redis.opsForList().leftPush(QUEUE_KEY, city);
    }

    /**
     * Waits up to {@code timeout} for a job. Returns the city, or null if none arrived.
     * This is BRPOP: it blocks the calling thread, it does not poll.
     */
    public String dequeue(Duration timeout) {
        return redis.opsForList().rightPop(QUEUE_KEY, timeout);
    }
}
