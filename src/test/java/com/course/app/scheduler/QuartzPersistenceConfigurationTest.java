package com.course.app.scheduler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzPersistenceConfigurationTest {

    @Test
    void disablesQuartzSchemaInitializationAndUsesPostgresJdbcStore() throws IOException {
        String configuration = resource("/application.yml");

        assertTrue(configuration.contains("job-store-type: jdbc"));
        assertTrue(configuration.contains("initialize-schema: never"));
        assertTrue(configuration.contains("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"));
        assertTrue(configuration.contains("useProperties: false"));
    }

    @Test
    void liquibaseChangeSetCreatesAllQuartzTablesWithIndexesAndForeignKeys() throws IOException {
        String master = resource("/db/changelog/db.changelog-master.yaml");
        String changeSet = resource("/db/changelog/002-create-quartz-tables.yaml");
        String schema = resource("/db/changelog/002-create-quartz-tables.sql");

        assertTrue(master.contains("002-create-quartz-tables.yaml"));
        assertTrue(changeSet.contains("002-create-quartz-tables.sql"));
        for (String table : new String[]{
                "QRTZ_JOB_DETAILS", "QRTZ_TRIGGERS", "QRTZ_SIMPLE_TRIGGERS", "QRTZ_CRON_TRIGGERS",
                "QRTZ_SIMPROP_TRIGGERS", "QRTZ_BLOB_TRIGGERS", "QRTZ_CALENDARS",
                "QRTZ_PAUSED_TRIGGER_GRPS", "QRTZ_FIRED_TRIGGERS", "QRTZ_SCHEDULER_STATE", "QRTZ_LOCKS"
        }) {
            assertTrue(schema.contains("CREATE TABLE " + table), "Missing Quartz table " + table);
        }
        assertTrue(schema.contains("CREATE INDEX IDX_QRTZ"));
        assertTrue(schema.contains("FOREIGN KEY"));
    }

    private static String resource(String name) throws IOException {
        try (var stream = QuartzPersistenceConfigurationTest.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
