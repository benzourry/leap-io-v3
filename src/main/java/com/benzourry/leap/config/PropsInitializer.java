package com.benzourry.leap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class PropsInitializer implements BeanPostProcessor, InitializingBean, EnvironmentAware {

    private JdbcTemplate jdbcTemplate;
    private ConfigurableEnvironment environment;
    private final String propertySourceName = "propertiesInsideDatabase";

    private static final Logger logger = LoggerFactory.getLogger(PropsInitializer.class);


    public PropsInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null != environment) {
            try {
                Map<String, Object> systemConfigMap = new HashMap<>();
                String sql = "SELECT k, v from key_value where g='app.prop' and enabled=1";
                List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);
                for (Map<String, Object> map : maps) {
                    String key = String.valueOf(map.get("k"));
                    Object value = map.get("v");
                    systemConfigMap.put(key, value);
                    logger.info("key={}, value={}", key, value);
                }
                environment.getPropertySources().addFirst(new MapPropertySource(propertySourceName, systemConfigMap));
            }catch(Exception e){}
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        if(environment instanceof ConfigurableEnvironment) {
            this.environment = (ConfigurableEnvironment) environment;
        }
    }
}
