package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.KeyValue;
import com.benzourry.leap.repository.KeyValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
public class KeyValueService {


    KeyValueRepository keyValueRepository;


    @Autowired
    public KeyValueService(KeyValueRepository keyValueRepository){
        this.keyValueRepository = keyValueRepository;
    }


    public KeyValue getByKey(String key){
        return keyValueRepository.findByKey(key);
    }


    public Map<String, Object> removeProp(Long id){
        keyValueRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Property", "Id", id));
        keyValueRepository.deleteById(id);
        return Map.of("success", true);
    }

    public Map<String, Object> removePropByGroupAndKey(String group,
                                                       String key){
        KeyValue kv = keyValueRepository.findByGroupAndKey(group, key).orElseThrow(()-> new ResourceNotFoundException("Property", "group+key", group+"+"+key));
        keyValueRepository.delete(kv);
        return Map.of("success", true);
    }
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
        return keyValueRepository.save(kv);
    }

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

    public Optional<String> getValue(String group, String key){
        return keyValueRepository.getValue(group, key);
    }
}
