package com.project.weather_insights.queue;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.project.weather_insights.service.WeatherService;

@Component
public class WeatherJobQueue {

    private final StringRedisTemplate redis;
    private static final String QUEUE_KEY = "weather-jobs";
    private static final String DEAD_LETTER_QUEUE_KEY = "weather-jobs-dead";
    
    public WeatherJobQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Long enqueue(String city) {

        return push(resolvedName(city));
    }

    public Long enqueue(String city, int attemptCount) {

        return push(resolvedName(city) + ":" + attemptCount);
    }

    private String resolvedName(String city) {

        return WeatherService.resolveCityOrThrow(city).name();
    }

    private Long push(String payload) {

        return redis.opsForList().leftPush(QUEUE_KEY, payload);
    }

    /**
     * Waits up to {@code timeout} for a job. Returns the city, or null if none arrived.
     * This is BRPOP: it blocks the calling thread, it does not poll.
     */
    public String dequeue(Duration timeout) {

        return redis.opsForList().rightPop(QUEUE_KEY, timeout);
    }

    public Long deadLetter(String city) {

        return redis.opsForList().leftPush(DEAD_LETTER_QUEUE_KEY, city);
    }

    /**
     * Moves every dead-lettered job back onto the main queue.
     * @return how many jobs were replayed (0 if the DLQ was empty)
     */
    public int replayAllDeadLetterQueueJobs() {
        int count = 0;
        while (true) {
            String job = redis.opsForList().rightPop(DEAD_LETTER_QUEUE_KEY);
            if (job == null) {
                break;
            }
            enqueue(job);
            count++;
        }
        return count;
    }
}
