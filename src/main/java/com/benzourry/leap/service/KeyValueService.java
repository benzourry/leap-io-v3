package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.KeyValue;
import com.benzourry.leap.repository.KeyValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
public class KeyValueService {


    KeyValueRepository keyValueRepository;

    ConfigurableEnvironment environment;

    private static final String DYNAMIC_PROPERTIES_SOURCE_NAME = "propertiesDynamic";

    private static final String APP_PROP = "app.prop";


    @Autowired
    public KeyValueService(KeyValueRepository keyValueRepository,
                           ConfigurableEnvironment environment){
        this.keyValueRepository = keyValueRepository;
        this.environment =  environment;
    }


    public KeyValue getByKey(String key){
        return keyValueRepository.findByKey(key);
    }


    public Map<String, Object> removeProp(Long id){
        KeyValue kv = keyValueRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Property", "Id", id));
        keyValueRepository.deleteById(id);
        if (APP_PROP.equals(kv.getGroup())){
            if (kv.getEnabled()==1){
                removeProperty(kv.getKey());
            }
        }
        return Map.of("success", true);
    }

    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public Map<String, Object> removePropByGroupAndKey(String group,
                                                       String key){
        KeyValue kv = keyValueRepository.findByGroupAndKey(group, key).orElseThrow(()-> new ResourceNotFoundException("Property", "group+key", group+"+"+key));
        keyValueRepository.delete(kv);
        if (APP_PROP.equals(kv.getGroup())){
            if (kv.getEnabled()==1){
                removeProperty(kv.getKey());
            }
        }
        return Map.of("success", true);
    }

    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public KeyValue save(String group,
                         String key,
                         KeyValue keyvalue){
        Optional<KeyValue> kvOpt = keyValueRepository.findByGroupAndKey(group, key);
        KeyValue kv;
        if (kvOpt.isPresent()){
            kv = kvOpt.get();
            kv.setValue(keyvalue.getValue());
            kv.setEnabled(keyvalue.getEnabled());
        }else{
            kv = keyvalue;
        }
        if (APP_PROP.equals(group)){
            if (kv.getEnabled()==1) {
                updateProperty(kv.getKey(), kv.getValue());
            }else{
                removeProperty(kv.getKey());
            }
        }
        return keyValueRepository.save(kv);
    }

    @CacheEvict(value = "platformKeyValuesStr", key = "#group + ':' + #key")
    public KeyValue saveValue(String group,
                         String key,
                         String value){
        Optional<KeyValue> kvOpt = keyValueRepository.findByGroupAndKey(group, key);
        KeyValue kv;
        if (kvOpt.isPresent()){
            kv = kvOpt.get();
            kv.setValue(value);
//            kv.setEnabled(keyvalue.getEnabled());
        }else{
            kv = new KeyValue(group, key, value, 1);
        }
        if (APP_PROP.equals(group)){
            if (kv.getEnabled()==1){
                updateProperty(kv.getKey(), kv.getValue());
            }
        }
        return keyValueRepository.save(kv);
    }

    public List<KeyValue> getAll(){
        return keyValueRepository.findAll();
    }


    public Map<String,List<KeyValue>> getAllGroup(){
        return keyValueRepository.findAll().stream()
                .collect(groupingBy(KeyValue::getGroup));
    }

    public List<KeyValue> getByGroup(String group){
        return keyValueRepository.findByGroup(group);
    }


    public Map getAllGroupMap(){
        return keyValueRepository.findAll().stream()
                .collect(groupingBy(e -> e.getGroup() ,
                        toMap(f-> f.getKey(), f-> Optional.ofNullable(f.getValue()).orElse(null))));
//                .collect(Collectors.toMap(KeyValue::getGroup, Collectors.toMap(KeyValue::getKey, KeyValue::getValue)));
    }


    public KeyValue getByGroupAndKey(String group, String key) {
        return keyValueRepository.findByGroupAndKey(group, key).orElseThrow(()->new ResourceNotFoundException("Property","group+key",group+"+"+key));
    }


    public void updateProperty(String key, String value){
        MutablePropertySources propertySources = environment.getPropertySources();
        if (!propertySources.contains(DYNAMIC_PROPERTIES_SOURCE_NAME)){
            Map<String, Object> dynamicProperties = new HashMap<>();
            dynamicProperties.put(key, value);
            propertySources.addFirst(new MapPropertySource(DYNAMIC_PROPERTIES_SOURCE_NAME, dynamicProperties));
        } else {
            MapPropertySource propertySource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTIES_SOURCE_NAME);
            propertySource.getSource().put(key, value);
        }
    }
    public void removeProperty(String key){
        MutablePropertySources propertySources = environment.getPropertySources();
        if (!propertySources.contains(DYNAMIC_PROPERTIES_SOURCE_NAME)){

            MapPropertySource propertySource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTIES_SOURCE_NAME);
            propertySource.getSource().remove(key);
        } else {
            System.out.println("No DYNAMIC PROPERTIES MAP found");
        }
    }

    public Optional<String> getValue(String group, String key){
        return keyValueRepository.getValue(group, key);
    }
}
