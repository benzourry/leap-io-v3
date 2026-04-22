package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.KeyValue;
import com.benzourry.leap.repository.KeyValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KeyValueService {

    private static final Logger logger = LoggerFactory.getLogger(KeyValueService.class);

    private final KeyValueRepository keyValueRepository;
    private final ConfigurableEnvironment environment;
    private final CacheManager cacheManager; // Added safely to handle cache eviction by ID

    private static final String DYNAMIC_PROPERTIES_SOURCE_NAME = "propertiesDynamic";
    private static final String APP_PROP = "app.prop";

    @Autowired
    public KeyValueService(KeyValueRepository keyValueRepository,
                           ConfigurableEnvironment environment,
                           CacheManager cacheManager) {
        this.keyValueRepository = keyValueRepository;
        this.environment = environment;
        this.cacheManager = cacheManager;
    }

    public KeyValue getByKey(String key) {
        return keyValueRepository.findByKey(key);
    }

    @Transactional
    public Map<String, Object> removeProp(Long id) {
        KeyValue kv = keyValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "Id", id));

        keyValueRepository.deleteById(id);

        if (APP_PROP.equals(kv.getGroup())) {
            if (kv.getEnabled() == 1) {
                removeProperty(kv.getKey());
            }
        }

        // Evict from cache programmatically since we only have the ID in the parameter
        Cache cache = cacheManager.getCache("platformKeyValuesStr");
        if (cache != null) {
            cache.evict(kv.getGroup() + ":" + kv.getKey());
        }

        return Map.of("success", true);
    }

    @Transactional
    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public Map<String, Object> removePropByGroupAndKey(String group, String key) {
        KeyValue kv = keyValueRepository.findByGroupAndKey(group, key)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "group+key", group + "+" + key));

        keyValueRepository.delete(kv);

        if (APP_PROP.equals(kv.getGroup())) {
            if (kv.getEnabled() == 1) {
                removeProperty(kv.getKey());
            }
        }
        return Map.of("success", true);
    }

    @Transactional
    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public KeyValue save(String group, String key, KeyValue keyvalue) {
        Optional<KeyValue> kvOpt = keyValueRepository.findByGroupAndKey(group, key);
        KeyValue kv;

        if (kvOpt.isPresent()) {
            kv = kvOpt.get();
            kv.setValue(keyvalue.getValue());
            kv.setEnabled(keyvalue.getEnabled());
        } else {
            kv = keyvalue;
        }

        if (APP_PROP.equals(group)) {
            if (kv.getEnabled() == 1) {
                updateProperty(kv.getKey(), kv.getValue());
            } else {
                removeProperty(kv.getKey());
            }
        }
        return keyValueRepository.save(kv);
    }

    @Transactional
    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public KeyValue saveValue(String group, String key, String value) {
        Optional<KeyValue> kvOpt = keyValueRepository.findByGroupAndKey(group, key);
        KeyValue kv;

        if (kvOpt.isPresent()) {
            kv = kvOpt.get();
            kv.setValue(value);
        } else {
            kv = new KeyValue(group, key, value, 1);
        }

        if (APP_PROP.equals(group)) {
            if (kv.getEnabled() == 1) {
                updateProperty(kv.getKey(), kv.getValue());
            }
        }
        return keyValueRepository.save(kv);
    }

    public List<KeyValue> getAll() {
        return keyValueRepository.findAll();
    }

    public Map<String, List<KeyValue>> getAllGroup() {
        Map<String, List<KeyValue>> map = new HashMap<>();
        for (KeyValue keyValue : keyValueRepository.findAll()) {
            map.computeIfAbsent(keyValue.getGroup(), k -> new ArrayList<>()).add(keyValue);
        }
        return map;
    }

    public List<KeyValue> getByGroup(String group) {
        return keyValueRepository.findByGroup(group);
    }

    public Map<String, Map<String, String>> getAllGroupMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        for (KeyValue e : keyValueRepository.findAll()) {
            map.computeIfAbsent(e.getGroup(), k -> new HashMap<>()).put(e.getKey(), e.getValue());
        }
        return map;
    }

    public KeyValue getByGroupAndKey(String group, String key) {
        return keyValueRepository.findByGroupAndKey(group, key)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "group+key", group + "+" + key));
    }

    public void updateProperty(String key, String value) {
        MutablePropertySources propertySources = environment.getPropertySources();

        if (propertySources.contains(DYNAMIC_PROPERTIES_SOURCE_NAME)) {
            MapPropertySource propertySource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTIES_SOURCE_NAME);
            if (propertySource != null) {
                propertySource.getSource().put(key, value);
            } else {
                logger.error("DYNAMIC PROPERTIES MAP is null");
            }
        } else {
            // Use ConcurrentHashMap for thread safety in dynamic environments
            Map<String, Object> dynamicProperties = new ConcurrentHashMap<>();
            dynamicProperties.put(key, value);
            propertySources.addFirst(new MapPropertySource(DYNAMIC_PROPERTIES_SOURCE_NAME, dynamicProperties));
        }
    }

    public void removeProperty(String key) {
        MutablePropertySources propertySources = environment.getPropertySources();

        // Fixed: The ! operator was removed. We only try to remove if it actually exists.
        if (propertySources.contains(DYNAMIC_PROPERTIES_SOURCE_NAME)) {
            MapPropertySource propertySource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTIES_SOURCE_NAME);
            if (propertySource != null) {
                propertySource.getSource().remove(key);
            } else {
                logger.error("DYNAMIC PROPERTIES MAP is null");
            }
        } else {
            logger.debug("Attempted to remove key '{}' but DYNAMIC PROPERTIES MAP not found", key);
        }
    }

    public Optional<String> getValue(String group, String key) {
        return keyValueRepository.getValue(group, key);
    }
}