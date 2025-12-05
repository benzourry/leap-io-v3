package com.benzourry.leap.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class ExecutorSchedulerConfig {

    private ThreadPoolTaskExecutor asyncExecutor;
    private ThreadPoolTaskScheduler scheduler;

//    public ExecutorSchedulerConfig() {
//        executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(7);
//        executor.setMaxPoolSize(42);
//        executor.setThreadNamePrefix("MyExec-");
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(30);
//        executor.initialize();
//    }
//
//    @Bean("asyncExec")
//    public Executor getAsyncExecutor() {
//        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
//    }
//
//
//    @PreDestroy
//    public void onDestroy() {
//        executor.shutdown();
//    }


    // ------------------------------
    // ASYNC EXECUTOR
    // ------------------------------
    @Bean("asyncExec")
    public Executor asyncExecutor() {
        asyncExecutor = new ThreadPoolTaskExecutor();
        asyncExecutor.setCorePoolSize(7);
        asyncExecutor.setMaxPoolSize(42);
        asyncExecutor.setQueueCapacity(200);
        asyncExecutor.setThreadNamePrefix("REKAAsyncExec-");
        asyncExecutor.setWaitForTasksToCompleteOnShutdown(true);
        asyncExecutor.setAwaitTerminationSeconds(60);
        asyncExecutor.initialize();

        return new DelegatingSecurityContextAsyncTaskExecutor(asyncExecutor);
    }

    // ------------------------------
    // SCHEDULER (for @Scheduled)
    // ------------------------------
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("REKAScheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        scheduler.initialize();

        return scheduler;
    }

    // ------------------------------
    // PROPER SHUTDOWN ON UNDEPLOY
    // ------------------------------
    @PreDestroy
    public void shutdownExecutors() {
        System.out.println("Shutting down async executor and scheduler...");

        if (asyncExecutor != null) {
            try {
                asyncExecutor.shutdown();
                System.out.println("Async Executor shutdown complete.");
            } catch (Exception e) {
                System.out.println("Error shutting down Async Executor: " + e);
            }
        }

        if (scheduler != null) {
            try {
                scheduler.shutdown();
                System.out.println("Scheduler shutdown complete.");
            } catch (Exception e) {
                System.out.println("Error shutting down Scheduler: " + e);
            }
        }

        System.out.println("Executors & Scheduler fully shut down.");
    }
}