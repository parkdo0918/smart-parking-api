package com.example.smartparkingapi.parking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${parking.total-spaces}")
    private int totalSpaces;

    private static final String OCCUPIED_COUNT_KEY = "parking:occupied_count";
    private static final String AVAILABLE_COUNT_KEY = "parking:available_count";
    private static final long CACHE_TTL_SECONDS = 60;  // 캐시 유효시간 60초

    /**
     * 주차 현황 캐시 업데이트
     */
    public void updateParkingStatus(long occupiedCount) {
        long availableCount = totalSpaces - occupiedCount;

        redisTemplate.opsForValue().set(OCCUPIED_COUNT_KEY, String.valueOf(occupiedCount), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(AVAILABLE_COUNT_KEY, String.valueOf(availableCount), CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("주차 현황 캐시 업데이트 - 사용 중: {}, 이용 가능: {}", occupiedCount, availableCount);
    }

    /**
     * 캐시에서 사용 중인 주차 공간 수 조회
     */
    public Long getOccupiedCount() {
        String value = redisTemplate.opsForValue().get(OCCUPIED_COUNT_KEY);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 캐시에서 이용 가능한 주차 공간 수 조회
     */
    public Long getAvailableCount() {
        String value = redisTemplate.opsForValue().get(AVAILABLE_COUNT_KEY);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 총 주차 공간 수 반환
     */
    public int getTotalSpaces() {
        return totalSpaces;
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        redisTemplate.delete(OCCUPIED_COUNT_KEY);
        redisTemplate.delete(AVAILABLE_COUNT_KEY);
        log.info("주차 현황 캐시 초기화 완료");
    }
}