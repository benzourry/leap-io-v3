package com.benzourry.leap.utility;
import com.benzourry.leap.model.Lambda;

public class LambdaExecutionContext {
    private static final ThreadLocal<Lambda> context = new ThreadLocal<>();

    public static void set(Lambda lambda) {
        context.set(lambda);
    }

    public static Lambda get() {
        return context.get();
    }

    public static void clear() {
        context.remove(); // CRITICAL to prevent memory leaks!
    }
}