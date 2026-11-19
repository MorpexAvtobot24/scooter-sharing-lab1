package com.course.app.scooter;

public record GpsTelemetry(String deviceId, String coordinates, boolean fallback) {

    public static GpsTelemetry unavailable() {
        return new GpsTelemetry("unavailable", "GPS telemetry unavailable", true);
    }
}
