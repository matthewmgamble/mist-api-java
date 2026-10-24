package ca.mgamble.mist.query;

import java.time.Duration;
import java.time.Instant;

/**
 * Query builder with Mist's {@code start} / {@code end} / {@code duration} time-range parameters.
 *
 * @param <S> the concrete builder type
 */
public abstract class TimeRangeQuery<S extends TimeRangeQuery<S>> extends ApiQuery<S> {

    public S start(Instant start) {
        return param("start", start);
    }

    /** Relative start such as {@code -1d} or {@code -2h}. */
    public S start(String relative) {
        return param("start", relative);
    }

    public S end(Instant end) {
        return param("end", end);
    }

    /** Relative end such as {@code -1h} or {@code now}. */
    public S end(String relative) {
        return param("end", relative);
    }

    /** Window length in Mist units, e.g. {@code 10m}, {@code 1d}, {@code 2w}. */
    public S duration(String duration) {
        return param("duration", duration);
    }

    /** Window length, converted to the largest whole Mist unit (rounded up to at least one minute). */
    public S duration(Duration duration) {
        return param("duration", toMistDuration(duration));
    }

    static String toMistDuration(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        long minutes = Math.max(1, (duration.toSeconds() + 59) / 60);
        if (minutes % (7 * 24 * 60) == 0) {
            return minutes / (7 * 24 * 60) + "w";
        }
        if (minutes % (24 * 60) == 0) {
            return minutes / (24 * 60) + "d";
        }
        if (minutes % 60 == 0) {
            return minutes / 60 + "h";
        }
        return minutes + "m";
    }
}
