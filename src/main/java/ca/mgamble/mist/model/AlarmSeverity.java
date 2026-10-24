package ca.mgamble.mist.model;

import java.util.Locale;

/** Alarm severities accepted by the alarm search endpoints. */
public enum AlarmSeverity implements ApiValue {
    CRITICAL("critical"),
    WARN("warn"),
    INFO("info"),
    UNKNOWN("unknown");

    private final String apiValue;

    AlarmSeverity(String apiValue) {
        this.apiValue = apiValue;
    }

    @Override
    public String apiValue() {
        return apiValue;
    }

    public static AlarmSeverity fromApi(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "critical" -> CRITICAL;
            case "warn" -> WARN;
            case "info" -> INFO;
            default -> UNKNOWN;
        };
    }
}
