package com.example.tryme.services;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final ConcurrentHashMap<String, List<?>> cache;

    public CacheService() {
        this.cache = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getFromCache(String cacheName, String key) {
        String fullKey = cacheName + "::" + key;
        return (List<T>) cache.get(fullKey);
    }

    public <T> void putToCache(String cacheName, String key, List<T> value) {
        String fullKey = cacheName + "::" + key;
        cache.put(fullKey, value);
    }

    public void clearCache(String cacheName) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(cacheName + "::"));
    }
}