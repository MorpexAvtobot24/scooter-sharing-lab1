package com.course.app.scooter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ScooterAsyncTasks {

    private static final Logger log = LoggerFactory.getLogger(ScooterAsyncTasks.class);

    private volatile String lastAuditThreadName;

    @Async("scooterTaskExecutor")
    public void sendScooterAuditNotificationAsync(Long scooterId) {
        lastAuditThreadName = Thread.currentThread().getName();
        pause();
        log.info("Async audit notification sent for scooter {}", scooterId);
    }

    @Async("scooterTaskExecutor")
    public CompletableFuture<GpsTelemetry> fetchGpsTelemetryAsync(Long scooterId) {
        pause();
        return CompletableFuture.completedFuture(
                new GpsTelemetry("GPS-" + scooterId, "lat=50.4501,lon=30.5234", false));
    }

    @Async("scooterTaskExecutor")
    public CompletableFuture<BatteryDiagnostics> fetchBatteryDiagnosticsAsync(Long scooterId) {
        pause();
        return CompletableFuture.completedFuture(new BatteryDiagnostics(95, "healthy", false));
    }

    public String getLastAuditThreadName() {
        return lastAuditThreadName;
    }

    private void pause() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Async scooter operation was interrupted", exception);
        }
    }
}
