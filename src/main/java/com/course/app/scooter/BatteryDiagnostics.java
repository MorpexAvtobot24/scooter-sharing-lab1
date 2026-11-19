package com.course.app.scooter;

public record BatteryDiagnostics(Integer healthPercent, String status, boolean fallback) {

    public static BatteryDiagnostics unavailable() {
        return new BatteryDiagnostics(null, "Battery diagnostics unavailable", true);
    }
}
