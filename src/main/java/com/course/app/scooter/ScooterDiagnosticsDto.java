package com.course.app.scooter;

public record ScooterDiagnosticsDto(
        Long scooterId,
        GpsTelemetry gpsTelemetry,
        BatteryDiagnostics batteryDiagnostics,
        boolean fallback
) {
}
