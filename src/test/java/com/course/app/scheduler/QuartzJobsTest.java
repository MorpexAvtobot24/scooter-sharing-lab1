package com.course.app.scheduler;

import com.course.app.scooter.ScooterMaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(OutputCaptureExtension.class)
class QuartzJobsTest {

    @Test
    void bothJobsDisallowConcurrentExecution() {
        assertTrue(LowBatteryMaintenanceJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
        assertTrue(ScooterFleetAuditJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
    }

    @Test
    void lowBatteryJobUsesDefaultAndTriggerOverrideFromMergedData() throws Exception {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);
        LowBatteryMaintenanceJob job = lowBatteryJob(service);
        JobDataMap defaults = new JobDataMap();
        defaults.put("batteryThreshold", 20);
        defaults.put("preserved", "keep");
        JobDataMap triggerValues = new JobDataMap();
        triggerValues.put("batteryThreshold", 15);
        JobDataMap merged = new JobDataMap(defaults);
        merged.putAll(triggerValues);

        job.execute(context(merged));

        verify(service).sendLowBatteryScootersToMaintenance(15);
        assertEquals("keep", merged.getString("preserved"));
    }

    @Test
    void lowBatteryJobUsesDocumentedDefaultWhenKeyIsMissing() throws Exception {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);

        lowBatteryJob(service).execute(context(new JobDataMap()));

        verify(service).sendLowBatteryScootersToMaintenance(20);
    }

    @Test
    void lowBatteryJobRejectsNonNumericAndNegativeThreshold() {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);
        LowBatteryMaintenanceJob job = lowBatteryJob(service);

        JobExecutionException nonNumeric = assertThrows(JobExecutionException.class,
                () -> job.execute(context(data("batteryThreshold", "low"))));
        JobExecutionException negative = assertThrows(JobExecutionException.class,
                () -> job.execute(context(data("batteryThreshold", -1))));

        assertTrue(nonNumeric.getMessage().contains("batteryThreshold"));
        assertTrue(negative.getMessage().contains("batteryThreshold"));
        verifyNoInteractions(service);
    }

    @Test
    void lowBatteryJobLogsServiceFailureAndDoesNotRefireImmediately(CapturedOutput output) throws Exception {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);
        IllegalStateException cause = new IllegalStateException("database unavailable");
        doThrow(cause).when(service).sendLowBatteryScootersToMaintenance(20);

        JobExecutionException exception = assertThrows(JobExecutionException.class,
                () -> lowBatteryJob(service).execute(context(new JobDataMap())));

        assertEquals(cause, exception.getCause());
        assertFalse(exception.refireImmediately());
        assertTrue(output.getOut().contains("Low-battery scooter maintenance job failed"));
        assertTrue(output.getOut().contains("database unavailable"));
        verify(service).sendLowBatteryScootersToMaintenance(20);
    }

    @Test
    void auditJobValidatesParametersAndUsesDefaultsWhenMissing() throws Exception {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);

        auditJob(service).execute(context(new JobDataMap()));

        verify(service).auditAndNormalizeFleetStatus("ALL", 100);
        JobExecutionException badScope = assertThrows(JobExecutionException.class,
                () -> auditJob(service).execute(context(data("auditScope", "unknown"))));
        JobExecutionException badLimit = assertThrows(JobExecutionException.class,
                () -> auditJob(service).execute(context(data("batchLimit", 0))));
        assertTrue(badScope.getMessage().contains("auditScope"));
        assertTrue(badLimit.getMessage().contains("batchLimit"));
    }

    @Test
    void auditJobPassesValidatedScopeAndLimit() throws Exception {
        ScooterMaintenanceService service = mock(ScooterMaintenanceService.class);
        JobDataMap values = new JobDataMap();
        values.put("auditScope", "active");
        values.put("batchLimit", "250");

        auditJob(service).execute(context(values));

        verify(service).auditAndNormalizeFleetStatus("ACTIVE", 250);
    }

    private static LowBatteryMaintenanceJob lowBatteryJob(ScooterMaintenanceService service) {
        LowBatteryMaintenanceJob job = new LowBatteryMaintenanceJob();
        ReflectionTestUtils.setField(job, "maintenanceService", service);
        return job;
    }

    private static ScooterFleetAuditJob auditJob(ScooterMaintenanceService service) {
        ScooterFleetAuditJob job = new ScooterFleetAuditJob();
        ReflectionTestUtils.setField(job, "maintenanceService", service);
        return job;
    }

    private static JobExecutionContext context(JobDataMap data) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        return context;
    }

    private static JobDataMap data(String key, Object value) {
        JobDataMap data = new JobDataMap();
        data.put(key, value);
        return data;
    }
}
