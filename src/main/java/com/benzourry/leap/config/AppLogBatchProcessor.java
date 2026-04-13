package com.benzourry.leap.config;

import com.benzourry.leap.model.AppLog;
import com.benzourry.leap.repository.AppLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class AppLogBatchProcessor {

    // Ultra-fast, non-blocking queue
    private static final ConcurrentLinkedQueue<AppLog> logQueue = new ConcurrentLinkedQueue<>();

    private final AppLogRepository appLogRepository;

    public AppLogBatchProcessor(AppLogRepository appLogRepository) {
        this.appLogRepository = appLogRepository;
    }

    // Your main services call this method. It is instant.
    public static void queueLog(AppLog log) {
        logQueue.add(log);
    }

    // Runs on a background thread every 2 seconds
    @Scheduled(fixedDelay = 2000)
    public void flushLogsToDatabase() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<AppLog> batch = new ArrayList<>();
        // Drain up to 1000 logs at a time to prevent memory spikes
        while (!logQueue.isEmpty() && batch.size() < 1000) {
            batch.add(logQueue.poll());
        }

        if (!batch.isEmpty()) {
            // Saves all logs in a single transaction
            appLogRepository.saveAll(batch);
        }
    }
}