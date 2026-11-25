package com.course.app.scooter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScooterMaintenanceServiceTest {

    @Mock
    private ScooterRepository repository;

    @Test
    void lowBatteryMaintenanceIsIdempotentAcrossConsecutiveRuns() {
        Scooter lowBattery = scooter(12, ScooterStatus.AVAILABLE);
        Scooter alreadyInMaintenance = scooter(10, ScooterStatus.MAINTENANCE);
        List<Scooter> fleet = new ArrayList<>(List.of(lowBattery, alreadyInMaintenance));
        when(repository.findByStatusInAndBatteryLevelLessThan(anyList(), anyInt())).thenAnswer(invocation ->
                fleet.stream()
                        .filter(scooter -> invocation.<List<ScooterStatus>>getArgument(0).contains(scooter.getStatus()))
                        .filter(scooter -> scooter.getBatteryLevel() < invocation.<Integer>getArgument(1))
                        .toList());
        ScooterMaintenanceService service = new ScooterMaintenanceService(repository);

        service.sendLowBatteryScootersToMaintenance(20);
        assertEquals(ScooterStatus.MAINTENANCE, lowBattery.getStatus());
        service.sendLowBatteryScootersToMaintenance(20);

        assertEquals(ScooterStatus.MAINTENANCE, lowBattery.getStatus());
        verify(repository, org.mockito.Mockito.times(2))
                .findByStatusInAndBatteryLevelLessThan(anyList(), anyInt());
        verify(repository).saveAll(List.of(lowBattery));
    }

    @Test
    void fleetAuditIsIdempotentAcrossConsecutiveRuns() {
        Scooter lowBattery = scooter(15, ScooterStatus.IN_USE);
        Scooter healthy = scooter(80, ScooterStatus.AVAILABLE);
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(lowBattery, healthy), invocation.getArgument(0), 2));
        ScooterMaintenanceService service = new ScooterMaintenanceService(repository);

        service.auditAndNormalizeFleetStatus("all", 25);
        service.auditAndNormalizeFleetStatus("ALL", 25);

        assertEquals(ScooterStatus.MAINTENANCE, lowBattery.getStatus());
        assertEquals(ScooterStatus.AVAILABLE, healthy.getStatus());
        verify(repository, org.mockito.Mockito.times(2)).findAll(any(Pageable.class));
        verify(repository).saveAll(List.of(lowBattery));
    }

    @Test
    void rejectsUnsupportedScopeAndInvalidBatchLimit() {
        ScooterMaintenanceService service = new ScooterMaintenanceService(repository);

        assertThrows(IllegalArgumentException.class, () -> service.auditAndNormalizeFleetStatus("unknown", 10));
        assertThrows(IllegalArgumentException.class, () -> service.auditAndNormalizeFleetStatus("ALL", 0));
        assertThrows(IllegalArgumentException.class, () -> service.auditAndNormalizeFleetStatus("ALL", 1001));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    private static Scooter scooter(int battery, ScooterStatus status) {
        Scooter scooter = new Scooter();
        scooter.setSerialNumber("SC-" + battery + "-" + status);
        scooter.setModel("Test");
        scooter.setBatteryLevel(battery);
        scooter.setPricePerMinute(new BigDecimal("0.20"));
        scooter.setStatus(status);
        return scooter;
    }
}
