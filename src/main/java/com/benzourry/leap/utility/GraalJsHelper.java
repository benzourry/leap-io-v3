package com.benzourry.leap.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GraalJsHelper {
    private static final Logger logger = LoggerFactory.getLogger(GraalJsHelper.class);

    private static final Engine SHARED_ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private static final HostAccess ACCESS = HostAccess.newBuilder(HostAccess.ALL)
            .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class)))
            .build();

    // 1. Initialize directly using a static method (Fixes the compiler error)
    private static final Source DAYJS_SOURCE = loadDayJsSource();

    // 2. Source Cache
    private static final ConcurrentHashMap<String, Source> SOURCE_CACHE = new ConcurrentHashMap<>();

    // 3. ThreadLocal Context
    private static final ThreadLocal<Context> THREAD_CONTEXT = ThreadLocal.withInitial(() -> {
        Context ctx = Context.newBuilder("js")
                .engine(SHARED_ENGINE)
                .allowHostAccess(ACCESS)
                .build();

        // Evaluate dayjs ONLY ONCE per thread
        if (DAYJS_SOURCE != null) {
            ctx.eval(DAYJS_SOURCE);
        }
        return ctx;
    });

    // Helper method to load the script safely with try-catch
    private static Source loadDayJsSource() {
        try {
            String dayjs = new String(new ClassPathResource("dayjs.min.js")
                    .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return Source.newBuilder("js", dayjs, "dayjs.js").buildLiteral();
        } catch (IOException e) {
            logger.error("Failed to load dayjs.min.js for GraalVM", e);
            return null;
        }
    }

    public static String execJs(String cacheId, String fn, Map<String, Object> bindingMaps, ObjectMapper mapper) {

        Source fnSource = SOURCE_CACHE.computeIfAbsent(cacheId, id -> {
            List<String> sortedKeys = new ArrayList<>(bindingMaps != null ? bindingMaps.keySet() : Collections.emptyList());
            Collections.sort(sortedKeys);

            StringBuilder varDeclarations = new StringBuilder();
            for (String key : sortedKeys) {
                varDeclarations.append("  var ").append(key).append(" = typeof __bind_").append(key)
                        .append(" !== 'undefined' && __bind_").append(key).append(" !== null ? JSON.parse(__bind_")
                        .append(key).append(") : null;\n");
            }

            String scriptWrapper = "function __runJs() {\n" + varDeclarations + "  return (" + fn + ");\n}";
            return Source.newBuilder("js", scriptWrapper, "execJs-" + id + ".js").buildLiteral();
        });

        try {
            // Retrieve the warm context for this thread
            Context ctx = THREAD_CONTEXT.get();

            ctx.eval(fnSource);
            Value bindings = ctx.getBindings("js");

            if (bindingMaps != null) {
                for (Map.Entry<String, Object> entry : bindingMaps.entrySet()) {
                    String jsonVal = null;
                    if (entry.getValue() != null) {
                        try {
                            jsonVal = mapper.writeValueAsString(entry.getValue());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to serialize binding key '" + entry.getKey() + "' to JSON", e);
                        }
                    }
                    bindings.putMember("__bind_" + entry.getKey(), jsonVal);
                }
            }

            Value runJs = bindings.getMember("__runJs");
            Value resultVal = runJs.execute();

            if (resultVal.isNull()) {
                return null;
            }

            Value jsonObj = bindings.getMember("JSON");
            Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);

            return jsonStrVal.isNull() ? null : jsonStrVal.asString();

        } catch (Exception e) {
            logger.error("Error executing JS snippet [cacheId={}]: {}", cacheId, e.getMessage(), e);
            throw new RuntimeException("JS execution failed for cacheId: " + cacheId, e);
        }
    }

    // Add these to GraalJsHelper.java
    public static Engine getSharedEngine() {
        return SHARED_ENGINE;
    }

    public static HostAccess getAccess() {
        return ACCESS;
    }

    public static Source getDayJsSource() {
        return DAYJS_SOURCE;
    }

    public static void cleanup() {
        THREAD_CONTEXT.remove();
    }
}