package com.course.app.scooter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ScooterMaintenanceService {

    /** Battery percentage used when a Quartz job has no batteryThreshold value. */
    public static final int DEFAULT_LOW_BATTERY_THRESHOLD = 20;
    /** Default page size and maximum supported page size for a fleet audit. */
    public static final int DEFAULT_AUDIT_BATCH_LIMIT = 100;
    public static final int MAX_AUDIT_BATCH_LIMIT = 1000;
    /** Scope used when a Quartz audit job has no auditScope value. */
    public static final String DEFAULT_AUDIT_SCOPE = "ALL";
    private static final List<ScooterStatus> ACTIVE_STATUSES =
            List.of(ScooterStatus.AVAILABLE, ScooterStatus.IN_USE);

    private final ScooterRepository repository;

    public ScooterMaintenanceService(ScooterRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void sendLowBatteryScootersToMaintenance(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("batteryThreshold must not be negative");
        }

        List<Scooter> scooters = repository.findByStatusInAndBatteryLevelLessThan(ACTIVE_STATUSES, threshold);
        scooters.stream()
                .filter(scooter -> scooter.getStatus() != ScooterStatus.MAINTENANCE)
                .forEach(scooter -> scooter.setStatus(ScooterStatus.MAINTENANCE));
        if (!scooters.isEmpty()) {
            repository.saveAll(scooters);
        }
    }

    @Transactional
    public void auditAndNormalizeFleetStatus(String scope, int limit) {
        String normalizedScope = normalizeScope(scope);
        if (limit < 1 || limit > MAX_AUDIT_BATCH_LIMIT) {
            throw new IllegalArgumentException("batchLimit must be between 1 and " + MAX_AUDIT_BATCH_LIMIT);
        }

        PageRequest page = PageRequest.of(0, limit, Sort.by("id").ascending());
        List<Scooter> scooters = normalizedScope.equals("ACTIVE")
                ? repository.findByStatusIn(ACTIVE_STATUSES, page)
                : repository.findAll(page).getContent();

        List<Scooter> changedScooters = scooters.stream()
                .filter(scooter -> ACTIVE_STATUSES.contains(scooter.getStatus()))
                .filter(scooter -> scooter.getBatteryLevel() < DEFAULT_LOW_BATTERY_THRESHOLD)
                .peek(scooter -> scooter.setStatus(ScooterStatus.MAINTENANCE))
                .toList();
        if (!changedScooters.isEmpty()) {
            repository.saveAll(changedScooters);
        }
    }

    public static String normalizeScope(String scope) {
        if (scope == null) {
            throw new IllegalArgumentException("auditScope must be ALL or ACTIVE");
        }
        String normalizedScope = scope.trim().toUpperCase(Locale.ROOT);
        if (!normalizedScope.equals("ALL") && !normalizedScope.equals("ACTIVE")) {
            throw new IllegalArgumentException("auditScope must be ALL or ACTIVE");
        }
        return normalizedScope;
    }
}
