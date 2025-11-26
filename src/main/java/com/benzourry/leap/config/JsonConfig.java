/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package com.benzourry.leap.config;

import com.benzourry.leap.utility.jsonresponse.JsonResponseAwareJsonMessageConverter;
import com.benzourry.leap.utility.jsonresponse.JsonResponseSupportFactoryBean;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 *
 * @author MohdRazif
 * based on the xml configuration by rkhairilzamrie
 * 
 */
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class JsonConfig {

//    @Autowired
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MappingJackson2HttpMessageConverter jsonConverter = new JsonResponseAwareJsonMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }

    /* For JSONMixin */
    @Bean
    public JsonResponseSupportFactoryBean jsonResponseSupportFactoryBean(){
        JsonResponseSupportFactoryBean jsonBean = new JsonResponseSupportFactoryBean();
        return jsonBean;
    }


//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        return JsonMapper.builder()
//                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
//                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//                .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
//                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
//                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
//                .build();
//    }


}