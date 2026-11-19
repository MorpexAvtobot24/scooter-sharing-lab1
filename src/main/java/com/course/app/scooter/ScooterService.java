package com.course.app.scooter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ScooterService {

    private final ScooterRepository repository;
    private final ScooterMapper mapper;
    private final ScooterAsyncTasks asyncTasks;
    private Duration diagnosticsTimeout = Duration.ofSeconds(2);

    @Autowired
    public ScooterService(ScooterRepository repository, ScooterMapper mapper, ScooterAsyncTasks asyncTasks) {
        this.repository = repository;
        this.mapper = mapper;
        this.asyncTasks = asyncTasks;
    }

    @Transactional(readOnly = true)
    @LogExecutionTime
    public List<ScooterDto> findAll() {
        return mapper.toDtos(repository.findAll(Sort.by("id")));
    }

    @Transactional(readOnly = true)
    @LogExecutionTime
    public ScooterDto findById(Long id) {
        return mapper.toDto(getScooter(id));
    }

    @Transactional(readOnly = true)
    public ScooterDiagnosticsDto getDiagnostics(Long id) {
        getScooter(id);
        CompletableFuture<GpsTelemetry> gpsFuture = fetchGpsSafely(id);
        CompletableFuture<BatteryDiagnostics> batteryFuture = fetchBatterySafely(id);
        return gpsFuture.thenCombine(batteryFuture, (gps, battery) ->
                        new ScooterDiagnosticsDto(id, gps, battery, gps.fallback() || battery.fallback()))
                .orTimeout(diagnosticsTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(exception ->
                        new ScooterDiagnosticsDto(
                                id, GpsTelemetry.unavailable(), BatteryDiagnostics.unavailable(), true))
                .join();
    }

    void setDiagnosticsTimeout(Duration diagnosticsTimeout) {
        this.diagnosticsTimeout = diagnosticsTimeout;
    }

    @AuditAdminAction
    @CheckBatteryOperation
    public ScooterDto create(ScooterDto scooterDto) {
        if (repository.existsBySerialNumber(scooterDto.serialNumber())) {
            throw new ScooterConflictException(scooterDto.serialNumber());
        }
        ScooterDto created = mapper.toDto(repository.save(mapper.toEntity(scooterDto)));
        asyncTasks.sendScooterAuditNotificationAsync(created.id());
        return created;
    }

    @AuditAdminAction
    @CheckBatteryOperation
    public ScooterDto update(Long id, ScooterDto scooterDto) {
        Scooter scooter = getScooter(id);
        if (repository.existsBySerialNumberAndIdNot(scooterDto.serialNumber(), id)) {
            throw new ScooterConflictException(scooterDto.serialNumber());
        }
        mapper.updateEntity(scooterDto, scooter);
        ScooterDto updated = mapper.toDto(repository.save(scooter));
        asyncTasks.sendScooterAuditNotificationAsync(updated.id());
        return updated;
    }

    @AuditAdminAction
    public void delete(Long id) {
        repository.delete(getScooter(id));
    }

    @LogExecutionTime
    public boolean isBatteryReadyForRental(Integer batteryLevel) {
        return batteryLevel != null && batteryLevel >= 20;
    }

    public boolean isBatteryReadyForRentalViaSelfInvocation(Integer batteryLevel) {
        return this.isBatteryReadyForRental(batteryLevel);
    }

    private Scooter getScooter(Long id) {
        return repository.findById(id).orElseThrow(() -> new ScooterNotFoundException(id));
    }

    private CompletableFuture<GpsTelemetry> fetchGpsSafely(Long id) {
        try {
            return asyncTasks.fetchGpsTelemetryAsync(id)
                    .exceptionally(exception -> GpsTelemetry.unavailable());
        } catch (TaskRejectedException exception) {
            return CompletableFuture.completedFuture(GpsTelemetry.unavailable());
        }
    }

    private CompletableFuture<BatteryDiagnostics> fetchBatterySafely(Long id) {
        try {
            return asyncTasks.fetchBatteryDiagnosticsAsync(id)
                    .exceptionally(exception -> BatteryDiagnostics.unavailable());
        } catch (TaskRejectedException exception) {
            return CompletableFuture.completedFuture(BatteryDiagnostics.unavailable());
        }
    }
}
