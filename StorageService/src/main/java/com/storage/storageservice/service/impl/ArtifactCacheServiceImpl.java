package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.service.ArtifactCacheService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ArtifactCacheServiceImpl implements ArtifactCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void put(ArtifactDto dto) {
        redisTemplate.opsForValue().set(dto.getId().toString(), dto, 1, TimeUnit.HOURS);
    }

    @Override
    public ArtifactDto get(String id) {
        return (ArtifactDto) redisTemplate.opsForValue().get(id);
    }

    @Override
    public void multiset(Collection<ArtifactDto> col) {
        Map<String, ArtifactDto> map = new HashMap<>();
        col.forEach(dto -> map.put(dto.getId().toString(), dto));

        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(@NonNull RedisOperations operations) {
                operations.opsForValue().multiSet(map);
                // Установка TTL для каждого ключа
                map.keySet().forEach(key ->
                        operations.expire(key, Duration.ofMinutes(3))
                );
                return null;
            }
        });
    }
}
