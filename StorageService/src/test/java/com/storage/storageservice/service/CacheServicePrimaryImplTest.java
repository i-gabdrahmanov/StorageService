package com.storage.storageservice.service;

import com.storage.storageservice.service.impl.CacheServicePrimaryImpl;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServicePrimaryImplTest {

    @Mock
    private MemcachedClient primaryCache;

    private CacheServicePrimaryImpl cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheServicePrimaryImpl(primaryCache);
    }

    @Test
    void setShouldStoreValueInCache() throws Exception {
        // Given
        String key = "testKey";
        Object value = "testValue";

        // When
        cacheService.set(key, value);

        // Then
        verify(primaryCache).set(eq(key), eq(3600), eq(value));
    }

    @Test
    void getShouldRetrieveValueFromCache() throws Exception {
        // Given
        String key = "testKey";
        Object expectedValue = "retrievedValue";
        when(primaryCache.get(anyString())).thenReturn(expectedValue);

        // When
        Object result = cacheService.get(key);

        // Then
        verify(primaryCache).get(eq(key));
        assertEquals(expectedValue, result);
    }

    @Test
    void deleteShouldRemoveValueFromCache() throws Exception {
        // Given
        String key = "testKey";

        // When
        cacheService.delete(key);

        // Then
        verify(primaryCache).delete(eq(key));
    }

    @Test
    void incrementShouldIncreaseValueInCache() throws Exception {
        // Given
        String key = "counter";
        long delta = 5L;
        long expectedIncrementedValue = 10L;
        when(primaryCache.incr(anyString(), eq(delta), eq(0L))).thenReturn(expectedIncrementedValue);

        // When
        long result = cacheService.increment(key, delta);

        // Then
        verify(primaryCache).incr(eq(key), eq(delta), eq(0L));
        assertEquals(expectedIncrementedValue, result);
    }

    @Test
    void setShouldHandleInterruptedException() throws Exception {
        // Given
        String key = "testKey";
        Object value = "testValue";
        doThrow(new InterruptedException()).when(primaryCache).set(anyString(), anyInt(), any());

        // When & Then
        assertThrows(InterruptedException.class, () -> cacheService.set(key, value));
    }

    @Test
    void setShouldHandleTimeoutException() throws Exception {
        // Given
        String key = "testKey";
        Object value = "testValue";
        doThrow(new TimeoutException()).when(primaryCache).set(anyString(), anyInt(), any());

        // When & Then
        assertThrows(TimeoutException.class, () -> cacheService.set(key, value));
    }

    @Test
    void setShouldHandleMemcachedException() throws Exception {
        // Given
        String key = "testKey";
        Object value = "testValue";
        doThrow(new MemcachedException("Cache error")).when(primaryCache).set(anyString(), anyInt(), any());

        // When & Then
        assertThrows(MemcachedException.class, () -> cacheService.set(key, value));
    }

    @Test
    void getShouldHandleInterruptedException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new InterruptedException()).when(primaryCache).get(anyString());

        // When & Then
        assertThrows(InterruptedException.class, () -> cacheService.get(key));
    }

    @Test
    void getShouldHandleTimeoutException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new TimeoutException()).when(primaryCache).get(anyString());

        // When & Then
        assertThrows(TimeoutException.class, () -> cacheService.get(key));
    }

    @Test
    void getShouldHandleMemcachedException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new MemcachedException("Cache error")).when(primaryCache).get(anyString());

        // When & Then
        assertThrows(MemcachedException.class, () -> cacheService.get(key));
    }

    @Test
    void deleteShouldHandleInterruptedException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new InterruptedException()).when(primaryCache).delete(anyString());

        // When & Then
        assertThrows(InterruptedException.class, () -> cacheService.delete(key));
    }

    @Test
    void deleteShouldHandleTimeoutException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new TimeoutException()).when(primaryCache).delete(anyString());

        // When & Then
        assertThrows(TimeoutException.class, () -> cacheService.delete(key));
    }

    @Test
    void deleteShouldHandleMemcachedException() throws Exception {
        // Given
        String key = "testKey";
        doThrow(new MemcachedException("Cache error")).when(primaryCache).delete(anyString());

        // When & Then
        assertThrows(MemcachedException.class, () -> cacheService.delete(key));
    }

    @Test
    void incrementShouldHandleInterruptedException() throws Exception {
        // Given
        String key = "counter";
        long delta = 5L;
        doThrow(new InterruptedException()).when(primaryCache).incr(anyString(), anyLong(), anyLong());

        // When & Then
        assertThrows(InterruptedException.class, () -> cacheService.increment(key, delta));
    }

    @Test
    void incrementShouldHandleTimeoutException() throws Exception {
        // Given
        String key = "counter";
        long delta = 5L;
        doThrow(new TimeoutException()).when(primaryCache).incr(anyString(), anyLong(), anyLong());

        // When & Then
        assertThrows(TimeoutException.class, () -> cacheService.increment(key, delta));
    }

    @Test
    void incrementShouldHandleMemcachedException() throws Exception {
        // Given
        String key = "counter";
        long delta = 5L;
        doThrow(new MemcachedException("Cache error")).when(primaryCache).incr(anyString(), anyLong(), anyLong());

        // When & Then
        assertThrows(MemcachedException.class, () -> cacheService.increment(key, delta));
    }
}