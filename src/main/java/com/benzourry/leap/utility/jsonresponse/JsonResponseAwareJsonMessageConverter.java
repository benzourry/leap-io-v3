package com.benzourry.leap.utility.jsonresponse;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map; // CHANGED: Added for caching
import java.util.concurrent.ConcurrentHashMap; // CHANGED: Added for caching

/**
 * Adds support for {@link my.unimas.iris.core.utility.jsonresponse.JsonResponse} annotation
 *
 * @author Jack Matthews
 *
 * Updated to prevent the ObjectMapper override.
 * Updated to return the new copy of ObjectMapper for each annotation during bootstrap
 * Updated to fix the problem with SPRING FRAMEWORK 4.2.1
 * (overrive the actual writeInternal from AbstractHttpMessageConverter instead of AbstractGenericHttpMessageConverter
 *
 * CHANGED:
 * - Added ConcurrentHashMap to cache custom ObjectMappers based on Mixin combinations.
 * - This prevents CPU and GC spikes caused by calling getObjectMapper().copy() on every HTTP request.
 */
public final class JsonResponseAwareJsonMessageConverter extends MappingJackson2HttpMessageConverter {

//    private final MappingJackson2HttpMessageConverter delegate = new MappingJackson2HttpMessageConverter();

    // CHANGED: Added a cache to hold pre-configured ObjectMappers.
    // Key is a string representation of the mixin combination.
    private final Map<String, ObjectMapper> objectMapperCache = new ConcurrentHashMap<>();

    public JsonResponseAwareJsonMessageConverter() {
        super();
    }

    @Override
    protected void writeInternal(Object object, Type type,
                                 HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {
        if (object instanceof ResponseWrapper) {
            writeJson((ResponseWrapper) object, outputMessage);
        } else {
            super.writeInternal(object, type, outputMessage);
        }
    }

    protected void writeJson(ResponseWrapper response, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {

        JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());

        // CHANGED: Removed the un-cached ObjectMapper copy that fired on every request:
        // ObjectMapper mapper = getObjectMapper().copy();

        JsonMixin[] jsonMixins = response.getJsonResponse().mixins();

        // CHANGED: Generate a unique cache key for this specific combination of mixins
        String cacheKey = generateCacheKey(jsonMixins);

        // CHANGED: Fetch from cache, or copy and configure ONCE and store it
        ObjectMapper mapper = objectMapperCache.computeIfAbsent(cacheKey, key -> {
            ObjectMapper newMapper = getObjectMapper().copy();

            // Add support for jackson mixins
            for (JsonMixin jsonMixin : jsonMixins) {
                newMapper.addMixIn(jsonMixin.target(), jsonMixin.mixin());
            }
            return newMapper;
        });

        // Use try-with-resources to ensure generator is closed properly
        try (JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(outputMessage.getBody(), encoding)) {
            mapper.writeValue(jsonGenerator, response.getOriginalResponse());
        } catch (IOException ex) {
            throw new HttpMessageNotWritableException(
                    "Failed to write JSON response: " + ex.getMessage(), ex
            );
        }

    }

    /**
     * CHANGED: Added helper method to generate a unique string key based on the target and mixin classes.
     * Example output: "com.example.Form=com.example.FormOne;com.example.Item=com.example.FormItemOne;"
     */
    private String generateCacheKey(JsonMixin[] mixins) {
        StringBuilder keyBuilder = new StringBuilder();
        for (JsonMixin mixin : mixins) {
            keyBuilder.append(mixin.target().getName())
                    .append('=')
                    .append(mixin.mixin().getName())
                    .append(';');
        }
        return keyBuilder.toString();
    }
}