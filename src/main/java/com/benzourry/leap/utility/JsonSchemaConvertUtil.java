/***
 * Codes by kelleszzz: https://github.com/kelleszzz
 * From the discussion in Langchain4J github page here: https://github.com/langchain4j/langchain4j/discussions/2789
 */

package com.benzourry.leap.utility;

//import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.model.chat.request.json.*;
import lombok.Data;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class JsonSchemaConvertUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public static JsonObjectSchema convertJsonSchema(String jsonSchema) {
        JsonSchema jsonSchemaObj = OBJECT_MAPPER.readValue(jsonSchema, JsonSchema.class);

        // only support object type
        if (!"object".equals(jsonSchemaObj.getType())) {
            return null;
        }

        return convertJsonSchema(jsonSchemaObj);
    }

    private static JsonObjectSchema convertJsonSchema(JsonSchema jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }

        JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
        builder.additionalProperties(jsonSchema.getAdditionalProperties());

        // required
        if (jsonSchema.getRequired() != null) {
            builder.required(jsonSchema.getRequired());
        }

        // properties
        Map<String, Object> originalProps = jsonSchema.getProperties();
        if (originalProps != null) {
            for (Map.Entry<String, Object> entry : originalProps.entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();

                JsonSchemaElement element = convertPropertySchema(propValue);
                if (element != null) {
                    builder.addProperty(propName, element);
                }
            }
        }

        return builder.build();
    }

    @SneakyThrows
    private static JsonSchemaElement convertPropertySchema(Object propValue) {
        if (propValue instanceof JsonSchema) {
            return convertSchemaElement((JsonSchema) propValue);
        } else if (propValue instanceof Map) {
            Map<String, Object> propMap = (Map<String, Object>) propValue;
            JsonSchema propSchema = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(propMap), JsonSchema.class);
            return convertSchemaElement(propSchema);
        }
        return null;
    }

    private static JsonSchemaElement convertSchemaElement(JsonSchema propSchema) {
        // 处理enumValues
        if (propSchema.getEnumValues() != null && !propSchema.getEnumValues().isEmpty()) {
            return JsonEnumSchema.builder()
                    .enumValues(propSchema.getEnumValues())
                    .build();
        }

        String type = propSchema.getType();
        if (type == null) {
            return null;
        }

        switch (type) {
            case "string":
                return new JsonStringSchema();
            case "integer":
                return new JsonIntegerSchema();
            case "number":
                return new JsonNumberSchema();
            case "boolean":
                return new JsonBooleanSchema();
            case "object":
                // recursively convert object
                return convertJsonSchema(propSchema);
            case "array":
                // array type
                if (propSchema.getItems() == null) {
                    return null;
                }
                JsonSchema items = propSchema.getItems();
                return new JsonArraySchema.Builder().items(convertJsonSchema(items)).build();
            default:
                return null;
        }
    }

    /**
     * JSON Schema Specification: https://json-schema.org/learn
     * Example: {"type":"object","properties":{"message":{"type":"string","description":"Input content"}}}
     * <p>
     * Usage Notes:
     * 1) Nested classes are not supported
     * 2) Generic types are not supported
     * 3) Minimize use of enum values
     * <p>
     * See io.modelcontextprotocol.spec.McpSchema.JsonSchema
     */
    @Data
    public static class JsonSchema {

        /**
         * Data type definition (required field)
         * Supported data types: https://json-schema.org/understanding-json-schema/reference/type
         * <p>Allowed values:
         * <ul>
         *   <li><b>string</b> - String type</li>
         *   <li><b>number</b> - Floating-point number type</li>
         *   <li><b>integer</b> - Integer type</li>
         *   <li><b>object</b> - Object type (key-value structure)</li>
         *   <li><b>array</b> - Array type</li>
         *   <li><b>boolean</b> - Boolean type</li>
         *   <li><b>null</b> - Null type</li>
         * </ul>
         *
         * @example {@code "type": "string"}
         * @see <a href="https://json-schema.org/understanding-json-schema/reference/type.html">JSON Schema Type Documentation</a>
         */
        private String type;

        private String description;

        /**
         * Enum type: https://json-schema.org/understanding-json-schema/reference/enum
         * Native JSON Schema uses "enum" field, but "enum" is a Java reserved keyword,
         * thus requires special handling
         */
//        @JSONField(name = "enum")
        @JsonProperty("enum")
        private List<String> enumValues;

        /**
         * Object property definitions (effective when type=object)
         * <p>Key-value pairs where keys represent property names and values are
         * complete JSON Schema definitions for those properties
         *
         * @example {@code "properties": { "name": { "type": "string" } }}
         * @see <a href="https://json-schema.org/understanding-json-schema/reference/object.html#properties">Property Definitions Documentation</a>
         */
        private Map<String, Object> properties;

        /**
         * Required property list (effective when type=object)
         * <p>Contains the set of property names that must be present
         *
         * @example {@code "required": ["id", "name"]}
         * @see <a href="https://json-schema.org/understanding-json-schema/reference/object.html#required-properties">Required Properties Documentation</a>
         */
        private List<String> required;

        /**
         * Additional properties allowance (effective when type=object)
         * <p>Controls schema behavior:
         * <ul>
         *   <li><b>true</b> - Allows properties not defined in 'properties'</li>
         *   <li><b>false</b> - Strictly prohibits undefined properties</li>
         * </ul>
         * Note: Can also define complex schemas to validate additional properties
         *
         * @example {@code "additionalProperties": false}
         * @see <a href="https://json-schema.org/understanding-json-schema/reference/object.html#additional-properties">Additional Properties Documentation</a>
         */
        private Boolean additionalProperties;


        /**
         * Array element definition (effective when type=array)
         */
        @Nullable
        private JsonSchema items;

    }

}