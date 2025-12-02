package com.benzourry.leap.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

//    @Bean
//    public CacheManager cacheManager() {
//        return new SimpleCacheManager();
//    }

    @Bean
    public CaffeineCacheManager cacheManager() {
        var manager = new CaffeineCacheManager();
        manager.setAsyncCacheMode(true); // if your app needs async
        return manager;
    }

}
