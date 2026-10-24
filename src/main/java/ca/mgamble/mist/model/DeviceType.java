package ca.mgamble.mist.model;

import java.util.Locale;

/** Mist device types. */
public enum DeviceType implements ApiValue {
    AP("ap", "Access Point"),
    SWITCH("switch", "Switch"),
    GATEWAY("gateway", "Gateway"),
    UNKNOWN("unknown", "Unknown");

    private final String apiValue;
    private final String displayName;

    DeviceType(String apiValue, String displayName) {
        this.apiValue = apiValue;
        this.displayName = displayName;
    }

    @Override
    public String apiValue() {
        return apiValue;
    }

    public String displayName() {
        return displayName;
    }

    /** Maps an API {@code type} value to an enum constant; anything unrecognised is {@link #UNKNOWN}. */
    public static DeviceType fromApi(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "ap" -> AP;
            case "switch" -> SWITCH;
            case "gateway" -> GATEWAY;
            default -> UNKNOWN;
        };
    }
}
