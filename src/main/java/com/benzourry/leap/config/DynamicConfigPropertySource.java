package com.benzourry.leap.config;

import com.benzourry.leap.model.DynamicConfig;
import com.benzourry.leap.repository.DynamicConfigRepository;
import org.springframework.core.env.EnumerablePropertySource;

public class DynamicConfigPropertySource extends EnumerablePropertySource<DynamicConfigRepository> {
    public DynamicConfigPropertySource(String name, DynamicConfigRepository source) {
        super(name, source);
    }

    @Override
    public String[] getPropertyNames() {
        return getSource().findAll().stream().map(DynamicConfig::getProp).toArray(String[]::new);
    }

    @Override
    public Object getProperty(String name) {
        return getSource().findByProp(name).getValue();
    }

}
