package com.benzourry.leap.utility.jsonresponse;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Adds support for {@link my.unimas.iris.core.utility.jsonresponse.JsonResponse} annotation
 *
 * @author Jack Matthews
 *
 * Updated to prevent the ObjectMapper override.
 * Updated to return the new copy of ObjectMapper for each annotation during bootstrap
 * Updated to fix the problem with SPRING FRAMEWORK 4.2.1
 * (overrive the actual writeInternal from AbstractHttpMessageConverter instead of AbstractGenericHttpMessageConverter
 */
public final class JsonResponseAwareJsonMessageConverter extends MappingJackson2HttpMessageConverter {

//    private final MappingJackson2HttpMessageConverter delegate = new MappingJackson2HttpMessageConverter();

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

        ObjectMapper mapper = getObjectMapper().copy();

        // Add support for jackson mixins
        JsonMixin[] jsonMixins = response.getJsonResponse().mixins();
        for (JsonMixin jsonMixin : jsonMixins) {
            mapper.addMixIn(jsonMixin.target(), jsonMixin.mixin());
        }

//        JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(outputMessage.getBody(), encoding);
//        try {
//            mapper.writeValue(jsonGenerator, response.getOriginalResponse());
//        } catch (IOException ex) {
//            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
//        }
        // Use try-with-resources to ensure generator is closed properly
        try (JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(outputMessage.getBody(), encoding)) {
            mapper.writeValue(jsonGenerator, response.getOriginalResponse());
        } catch (IOException ex) {
            throw new HttpMessageNotWritableException(
                    "Failed to write JSON response: " + ex.getMessage(), ex
            );
        }

    }

}