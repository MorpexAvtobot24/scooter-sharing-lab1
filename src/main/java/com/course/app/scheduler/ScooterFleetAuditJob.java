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
public class ScooterFleetAuditJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ScooterFleetAuditJob.class);

    @Autowired
    private ScooterMaintenanceService maintenanceService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        String scope = JobDataMapValues.auditScope(data, ScooterMaintenanceService.DEFAULT_AUDIT_SCOPE);
        int limit = JobDataMapValues.integer(data, "batchLimit",
                ScooterMaintenanceService.DEFAULT_AUDIT_BATCH_LIMIT, 1,
                ScooterMaintenanceService.MAX_AUDIT_BATCH_LIMIT);
        try {
            maintenanceService.auditAndNormalizeFleetStatus(scope, limit);
        } catch (Exception exception) {
            logger.error("Scooter fleet audit job failed", exception);
            throw new JobExecutionException(exception, false);
        }
    }
}
