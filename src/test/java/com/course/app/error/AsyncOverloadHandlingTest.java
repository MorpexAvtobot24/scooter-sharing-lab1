package com.course.app.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncOverloadHandlingTest {

    @Test
    void saturatedExecutorRejectsExcessTaskAndHandlerReturns429() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("scooter-test-");
        executor.initialize();

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                taskStarted.countDown();
                try {
                    releaseTask.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(taskStarted.await(2, TimeUnit.SECONDS));
            executor.execute(() -> {
            });

            TaskRejectedException rejection = assertThrows(
                    TaskRejectedException.class, () -> executor.execute(() -> {
                    }));
            var response = new GlobalExceptionHandler().handleTaskRejected(rejection);

            assertEquals(429, response.getStatusCode().value());
            assertEquals("Сервер перевантажений, спробуйте пізніше",
                    response.getBody().message());
        } finally {
            releaseTask.countDown();
            executor.shutdown();
        }
    }
}
