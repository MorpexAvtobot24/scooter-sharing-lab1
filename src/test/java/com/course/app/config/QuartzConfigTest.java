package com.course.app.config;

import com.course.app.scheduler.LowBatteryMaintenanceJob;
import com.course.app.scheduler.ScooterFleetAuditJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzConfigTest {

    @Test
    void declaresDurableJobsAndAllExpectedTriggers() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(QuartzConfig.class)) {
            Map<String, JobDetail> jobs = context.getBeansOfType(JobDetail.class);
            Map<String, Trigger> triggers = context.getBeansOfType(Trigger.class);

            assertEquals(2, jobs.size());
            assertEquals(3, triggers.size());

            JobDetail maintenanceJob = context.getBean("lowBatteryMaintenanceJobDetail", JobDetail.class);
            assertEquals("low-battery-maintenance", maintenanceJob.getKey().getName());
            assertEquals("scooter-maintenance", maintenanceJob.getKey().getGroup());
            assertEquals(LowBatteryMaintenanceJob.class, maintenanceJob.getJobClass());
            assertTrue(maintenanceJob.isDurable());
            assertEquals(20, maintenanceJob.getJobDataMap().getInt("batteryThreshold"));

            JobDetail auditJob = context.getBean("scooterFleetAuditJobDetail", JobDetail.class);
            assertEquals("scooter-fleet-audit", auditJob.getKey().getName());
            assertEquals("scooter-audit", auditJob.getKey().getGroup());
            assertEquals(ScooterFleetAuditJob.class, auditJob.getJobClass());
            assertTrue(auditJob.isDurable());
            assertEquals("ALL", auditJob.getJobDataMap().getString("auditScope"));
            assertEquals(100, auditJob.getJobDataMap().getInt("batchLimit"));

            Trigger daytime = context.getBean("lowBatteryDaytimeTrigger", Trigger.class);
            assertTrigger(daytime, "low-battery-daytime", "scooter-maintenance", maintenanceJob.getKey());
            assertEquals(15, daytime.getJobDataMap().getInt("batteryThreshold"));

            Trigger nighttime = context.getBean("lowBatteryNighttimeTrigger", Trigger.class);
            assertTrigger(nighttime, "low-battery-nighttime", "scooter-maintenance", maintenanceJob.getKey());
            assertEquals(30, nighttime.getJobDataMap().getInt("batteryThreshold"));

            Trigger audit = context.getBean("scooterFleetAuditTrigger", Trigger.class);
            assertTrigger(audit, "scooter-fleet-audit-nightly", "scooter-audit", auditJob.getKey());
            assertEquals("ACTIVE", audit.getJobDataMap().getString("auditScope"));
            assertEquals(100, audit.getJobDataMap().getInt("batchLimit"));
        }
    }

    @Test
    void everyTriggerHasAnExplicitMisfirePolicyAndCronTimezone() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(QuartzConfig.class)) {
            for (Trigger trigger : context.getBeansOfType(Trigger.class).values()) {
                assertInstanceOf(CronTrigger.class, trigger);
                CronTrigger cronTrigger = (CronTrigger) trigger;
                assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cronTrigger.getMisfireInstruction());
                assertNotNull(cronTrigger.getTimeZone());
                assertFalse(cronTrigger.getTimeZone().getID().isBlank());
            }
            assertEquals("Europe/Kyiv", context.getBean("lowBatteryDaytimeTrigger", CronTrigger.class)
                    .getTimeZone().getID());
            assertEquals("UTC", context.getBean("lowBatteryNighttimeTrigger", CronTrigger.class)
                    .getTimeZone().getID());
            assertEquals("Europe/Kyiv", context.getBean("scooterFleetAuditTrigger", CronTrigger.class)
                    .getTimeZone().getID());
        }
    }

    @Test
    void triggerDataOverridesDefaultsAndLeavesOtherJobDataIntact() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(QuartzConfig.class)) {
            JobDataMap defaults = context.getBean("lowBatteryMaintenanceJobDetail", JobDetail.class).getJobDataMap();
            defaults.put("preserved", "job-default");
            JobDataMap triggerData = context.getBean("lowBatteryDaytimeTrigger", Trigger.class).getJobDataMap();
            JobDataMap merged = new JobDataMap(defaults);
            merged.putAll(triggerData);

            assertEquals(15, merged.getInt("batteryThreshold"));
            assertEquals("job-default", merged.getString("preserved"));
        }
    }

    private static void assertTrigger(Trigger trigger, String name, String group, JobKey jobKey) {
        assertEquals(name, trigger.getKey().getName());
        assertEquals(group, trigger.getKey().getGroup());
        assertEquals(jobKey, trigger.getJobKey());
    }
}
