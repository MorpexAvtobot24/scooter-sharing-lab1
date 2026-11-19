package com.course.app.scooter;

import com.course.app.async.ScooterAsyncExceptionHandler;
import com.course.app.config.AsyncConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AsyncConfig.class,
        ScooterAsyncTasks.class,
        ScooterAsyncTasksTest.FailingVoidTaskConfiguration.class
})
class ScooterAsyncTasksTest {

    @Autowired
    private ScooterAsyncTasks asyncTasks;

    @Autowired
    private ScooterAsyncExceptionHandler exceptionHandler;

    @Autowired
    private FailingVoidTask failingVoidTask;

    @Test
    void fireAndForget_returnsImmediatelyAndRunsOnNamedExecutorThread() throws Exception {
        long startNanos = System.nanoTime();

        asyncTasks.sendScooterAuditNotificationAsync(4L);

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        assertTrue(elapsedMillis < 300, "Fire-and-forget call should not wait for its 600ms side effect");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (asyncTasks.getLastAuditThreadName() == null && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertNotNull(asyncTasks.getLastAuditThreadName());
        assertTrue(asyncTasks.getLastAuditThreadName().startsWith("scooter-async-"));
    }

    @Test
    void telemetryCalls_runConcurrentlyAndProduceBothResults() throws Exception {
        long startNanos = System.nanoTime();

        CompletableFuture<GpsTelemetry> gpsFuture = asyncTasks.fetchGpsTelemetryAsync(5L);
        CompletableFuture<BatteryDiagnostics> batteryFuture =
                asyncTasks.fetchBatteryDiagnosticsAsync(5L);
        GpsTelemetry gps = gpsFuture.get(2, TimeUnit.SECONDS);
        BatteryDiagnostics battery = batteryFuture.get(2, TimeUnit.SECONDS);

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        assertTrue(elapsedMillis < 1_050,
                "Two 600ms tasks should complete in parallel rather than taking their summed duration");
        assertEquals("GPS-5", gps.deviceId());
        assertEquals(95, battery.healthPercent());
    }

    @Test
    void uncaughtVoidAsyncException_isRecordedByConfiguredHandler() throws Exception {
        failingVoidTask.failAsync();

        assertTrue(exceptionHandler.awaitFailure(2, TimeUnit.SECONDS));
        ScooterAsyncExceptionHandler.AsyncFailure failure = exceptionHandler.getLastFailure();
        assertEquals("failAsync", failure.methodName());
        assertEquals("expected async failure", failure.exception().getMessage());
    }

    @Configuration
    static class FailingVoidTaskConfiguration {
        @Bean
        FailingVoidTask failingVoidTask() {
            return new FailingVoidTask();
        }
    }

    static class FailingVoidTask {
        @Async("scooterTaskExecutor")
        public void failAsync() {
            throw new IllegalStateException("expected async failure");
        }
    }
}
