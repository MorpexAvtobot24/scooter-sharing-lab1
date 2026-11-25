package com.course.app.scheduler;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import java.math.BigDecimal;

final class JobDataMapValues {

    private JobDataMapValues() {
    }

    static int integer(JobDataMap data, String key, int defaultValue, int minimum, int maximum)
            throws JobExecutionException {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        Object value = data.get(key);
        try {
            BigDecimal number = value instanceof Number numeric
                    ? new BigDecimal(numeric.toString())
                    : value instanceof String text ? new BigDecimal(text.trim()) : null;
            if (number == null) {
                throw new NumberFormatException("not a number");
            }
            int parsed = number.intValueExact();
            if (parsed < minimum || parsed > maximum) {
                throw new NumberFormatException("outside the allowed range");
            }
            return parsed;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new JobExecutionException(
                    "Invalid JobDataMap value for '" + key + "': expected an integer between "
                            + minimum + " and " + maximum + ", got '" + value + "'.");
        }
    }

    static String auditScope(JobDataMap data, String defaultValue) throws JobExecutionException {
        if (!data.containsKey("auditScope")) {
            return defaultValue;
        }
        Object value = data.get("auditScope");
        if (!(value instanceof String scope)) {
            throw new JobExecutionException("Invalid JobDataMap value for 'auditScope': expected ALL or ACTIVE.");
        }
        String normalized = scope.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("ALL") && !normalized.equals("ACTIVE")) {
            throw new JobExecutionException("Invalid JobDataMap value for 'auditScope': expected ALL or ACTIVE.");
        }
        return normalized;
    }
}
