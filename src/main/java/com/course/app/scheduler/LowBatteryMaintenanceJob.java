package com.course.app.scheduler;

import com.course.app.scooter.ScooterMaintenanceService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class LowBatteryMaintenanceJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(LowBatteryMaintenanceJob.class);

    @Autowired
    private ScooterMaintenanceService maintenanceService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int threshold = JobDataMapValues.integer(
                data, "batteryThreshold", ScooterMaintenanceService.DEFAULT_LOW_BATTERY_THRESHOLD, 0,
                Integer.MAX_VALUE);
        try {
            maintenanceService.sendLowBatteryScootersToMaintenance(threshold);
        } catch (Exception exception) {
            logger.error("Low-battery scooter maintenance job failed", exception);
            throw new JobExecutionException(exception, false);
        }
    }
}
