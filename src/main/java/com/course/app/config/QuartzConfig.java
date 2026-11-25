package com.course.app.config;

import com.course.app.scheduler.LowBatteryMaintenanceJob;
import com.course.app.scheduler.ScooterFleetAuditJob;
import com.course.app.scooter.ScooterMaintenanceService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.quartz.spi.TriggerFiredBundle;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    public static final String MAINTENANCE_JOB_NAME = "low-battery-maintenance";
    public static final String MAINTENANCE_JOB_GROUP = "scooter-maintenance";
    public static final String AUDIT_JOB_NAME = "scooter-fleet-audit";
    public static final String AUDIT_JOB_GROUP = "scooter-audit";

    @Bean
    public JobDetail lowBatteryMaintenanceJobDetail() {
        JobDataMap defaults = new JobDataMap();
        defaults.put("batteryThreshold", ScooterMaintenanceService.DEFAULT_LOW_BATTERY_THRESHOLD);
        return JobBuilder.newJob(LowBatteryMaintenanceJob.class)
                .withIdentity(MAINTENANCE_JOB_NAME, MAINTENANCE_JOB_GROUP)
                .usingJobData(defaults)
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger lowBatteryDaytimeTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity("low-battery-daytime", MAINTENANCE_JOB_GROUP)
                .forJob(new JobKey(MAINTENANCE_JOB_NAME, MAINTENANCE_JOB_GROUP))
                .usingJobData("batteryThreshold", 15)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Europe/Kyiv"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public CronTrigger lowBatteryNighttimeTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity("low-battery-nighttime", MAINTENANCE_JOB_GROUP)
                .forJob(new JobKey(MAINTENANCE_JOB_NAME, MAINTENANCE_JOB_GROUP))
                .usingJobData("batteryThreshold", 30)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("UTC"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public JobDetail scooterFleetAuditJobDetail() {
        JobDataMap defaults = new JobDataMap();
        defaults.put("auditScope", ScooterMaintenanceService.DEFAULT_AUDIT_SCOPE);
        defaults.put("batchLimit", ScooterMaintenanceService.DEFAULT_AUDIT_BATCH_LIMIT);
        return JobBuilder.newJob(ScooterFleetAuditJob.class)
                .withIdentity(AUDIT_JOB_NAME, AUDIT_JOB_GROUP)
                .usingJobData(defaults)
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger scooterFleetAuditTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity("scooter-fleet-audit-nightly", AUDIT_JOB_GROUP)
                .forJob(new JobKey(AUDIT_JOB_NAME, AUDIT_JOB_GROUP))
                .usingJobData("auditScope", "ACTIVE")
                .usingJobData("batchLimit", 100)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Europe/Kyiv"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(
            AutowireCapableBeanFactory beanFactory) {
        return schedulerFactory -> schedulerFactory.setJobFactory(new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        });
    }
}
