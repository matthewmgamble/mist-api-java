package ca.mgamble.mist;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conversions between device MAC addresses and Mist device IDs. */
public final class MistIds {

    private static final String DEVICE_ID_PREFIX = "00000000-0000-0000-1000-";
    private static final Pattern MAC = Pattern.compile("[0-9a-f]{12}");

    private MistIds() {
    }

    /**
     * Normalises a MAC address to Mist's form: 12 lowercase hex digits, no separators.
     *
     * @throws IllegalArgumentException if it is not a MAC address
     */
    public static String normalizeMac(String mac) {
        String normalized = mac.toLowerCase(Locale.ROOT).replaceAll("[:.\\-\\s]", "");
        if (!MAC.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Not a MAC address: " + mac);
        }
        return normalized;
    }

    /** Mist device IDs embed the MAC: {@code 5c5b35d0077b} becomes {@code 00000000-0000-0000-1000-5c5b35d0077b}. */
    public static String deviceIdFromMac(String mac) {
        return DEVICE_ID_PREFIX + normalizeMac(mac);
    }

    /** The MAC embedded in a device ID. */
    public static String macFromDeviceId(String deviceId) {
        if (!deviceId.toLowerCase(Locale.ROOT).startsWith(DEVICE_ID_PREFIX)) {
            throw new IllegalArgumentException("Not a Mist device ID: " + deviceId);
        }
        return normalizeMac(deviceId.substring(DEVICE_ID_PREFIX.length()));
    }
}
