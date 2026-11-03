package com.benzourry.leap.service.storage;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StorageFactory {

    private final Map<String, StorageProvider> providers;

    public StorageFactory(List<StorageProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(StorageProvider::getType, Function.identity()));
    }

    public StorageProvider getProvider(String type) {
        if (type == null || type.isBlank() || !providers.containsKey(type.toLowerCase())) {
            return providers.get("local"); // Fallback to local
        }
        return providers.get(type.toLowerCase());
    }
}