package com.ray.knowledgeflow.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** 应用级有界异步执行器，避免默认执行器和按请求创建线程造成资源失控。 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        return executor("kflow-async-", 4, 16, 500, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean(name = "aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        return executor("kflow-ai-", 2, 8, 100, new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean(name = "searchTaskExecutor")
    public ThreadPoolTaskExecutor searchTaskExecutor() {
        return executor("kflow-search-", 4, 12, 200, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                org.slf4j.LoggerFactory.getLogger(method.getDeclaringClass())
                        .error("未处理的异步任务异常: method={}", method.getName(), throwable);
    }

    private ThreadPoolTaskExecutor executor(String prefix, int coreSize, int maxSize, int queueCapacity,
            java.util.concurrent.RejectedExecutionHandler rejectionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(rejectionHandler);
        return executor;
    }
}
