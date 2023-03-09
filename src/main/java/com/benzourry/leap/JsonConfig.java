/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package com.benzourry.leap;

import com.benzourry.leap.utility.jsonresponse.JsonResponseAwareJsonMessageConverter;
import com.benzourry.leap.utility.jsonresponse.JsonResponseSupportFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 *
 * @author MohdRazif
 * based on the xml configuration by rkhairilzamrie
 * 
 */
@Configuration
public class JsonConfig {

    @Autowired
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
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

}