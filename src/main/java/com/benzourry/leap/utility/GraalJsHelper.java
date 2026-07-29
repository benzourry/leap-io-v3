package com.benzourry.leap.utility;

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

    // SECURITY FIX: Changed from HostAccess.ALL to HostAccess.NONE
    // Prevents Remote Code Execution (RCE) via Java Reflection in JS snippets.
    private static final HostAccess ACCESS = HostAccess.newBuilder(HostAccess.NONE)
            .allowMapAccess(true)
            .allowListAccess(true)
            .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class)))
            .build();

    private static final Source DAYJS_SOURCE = loadDayJsSource();
    private static final ConcurrentHashMap<String, Source> SOURCE_CACHE = new ConcurrentHashMap<>();

    private static final ThreadLocal<Context> THREAD_CONTEXT = ThreadLocal.withInitial(() -> {
        Context ctx = Context.newBuilder("js")
                .engine(SHARED_ENGINE)
                .allowHostAccess(ACCESS)
                .build();

        if (DAYJS_SOURCE != null) {
            ctx.eval(DAYJS_SOURCE);
        }
        return ctx;
    });

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

    public static Object execJs(String cacheId, String fn, Map<String, Object> bindingMaps) {

        // CACHE FIX: Generate a strict signature based on the exact variable names provided.
        // Prevents mapping the wrong values to variables if map keys change between executions.
        List<String> sortedKeys = new ArrayList<>(bindingMaps != null ? bindingMaps.keySet() : Collections.emptyList());
        Collections.sort(sortedKeys);
        String signature = String.join(",", sortedKeys);

        // Example strictCacheId: "mailer-pre-5[$$,$$_,$,$now$,$prev$]"
        String strictCacheId = cacheId + "[" + signature + "]";

        Source fnSource = SOURCE_CACHE.computeIfAbsent(strictCacheId, id -> {
            String argsList = String.join(", ", sortedKeys);
            String scriptWrapper = "(function(" + argsList + ") { return (" + fn + "); })";
            return Source.newBuilder("js", scriptWrapper, "execJs-" + id + ".js").buildLiteral();
        });

        try {
            Context ctx = THREAD_CONTEXT.get();
            Value jsFunction = ctx.eval(fnSource);

            Object[] args = new Object[0];
            if (bindingMaps != null) {
                args = new Object[sortedKeys.size()];
                for (int i = 0; i < sortedKeys.size(); i++) {
                    args[i] = bindingMaps.get(sortedKeys.get(i));
                }
            }

            Value resultVal = jsFunction.execute(args);

            if (resultVal.isNull()) {
                return null;
            }

            return resultVal.as(Object.class);

        } catch (Exception e) {
            logger.error("Error executing JS snippet [cacheId={}]: {}", strictCacheId, e.getMessage(), e);
            throw new RuntimeException("JS execution failed for cacheId: " + strictCacheId, e);
        }
    }

    // MEMORY LEAK FIX: Added ctx.close(true)
    public static void cleanup() {
        Context ctx = THREAD_CONTEXT.get();
        if (ctx != null) {
            try {
                ctx.close(true); // Force closes the context to free memory
            } catch (Exception e) {
                logger.warn("Failed to close GraalVM context", e);
            }
        }
        THREAD_CONTEXT.remove(); // Removes the ThreadLocal reference
    }

    public static Engine getSharedEngine() {
        return SHARED_ENGINE;
    }

    public static HostAccess getAccess() {
        return ACCESS;
    }

    public static Source getDayJsSource() {
        return DAYJS_SOURCE;
    }
}