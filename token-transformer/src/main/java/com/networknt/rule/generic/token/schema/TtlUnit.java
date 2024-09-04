package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.TimeUnit;

public enum TtlUnit {

    @JsonProperty("nanosecond")
    @JsonAlias({"nano", "n", "Nano", "Nanosecond"})
    NANOSECOND,

    @JsonProperty("microsecond")
    @JsonAlias({"micro", "us", "Micro", "Microsecond"})
    MICROSECOND,

    @JsonProperty("millisecond")
    @JsonAlias({"milli", "ms", "msec", "Millisecond", "Milli"})
    MILLISECOND,

    @JsonProperty("second")
    @JsonAlias({"sec", "s", "Second", "Sec"})
    SECOND,

    @JsonProperty("minute")
    @JsonAlias({"min", "m", "Minute", "Min"})
    MINUTE,

    @JsonProperty("hour")
    @JsonAlias({"hr", "h", "Hour", "Hr"})
    HOUR,

    @JsonProperty("day")
    @JsonAlias({"d", "Day"})
    DAY;

    public long unitToMillis(final long unitTime) {
        switch (this) {
            case NANOSECOND:
                return TimeUnit.NANOSECONDS.toMillis(unitTime);
            case MICROSECOND:
                return TimeUnit.MICROSECONDS.toMillis(unitTime);
            case MILLISECOND:
                return unitTime;
            case SECOND:
                return TimeUnit.SECONDS.toMillis(unitTime);
            case MINUTE:
                return TimeUnit.MINUTES.toMillis(unitTime);
            case HOUR:
                return TimeUnit.HOURS.toMillis(unitTime);
            case DAY:
                return TimeUnit.DAYS.toMillis(unitTime);
            default:
                throw new IllegalStateException("Invalid enum");
        }
    }

    public long millisToUnit(final long millisTime) {
        switch (this) {
            case NANOSECOND:
                return TimeUnit.MILLISECONDS.toNanos(millisTime);
            case MICROSECOND:
                return TimeUnit.MILLISECONDS.toMicros(millisTime);
            case MILLISECOND:
                return millisTime;
            case SECOND:
                return TimeUnit.MILLISECONDS.toSeconds(millisTime);
            case MINUTE:
                return TimeUnit.MILLISECONDS.toMinutes(millisTime);
            case HOUR:
                return TimeUnit.MILLISECONDS.toHours(millisTime);
            case DAY:
                return TimeUnit.MILLISECONDS.toDays(millisTime);
            default:
                throw new IllegalStateException("Invalid enum");
        }
    }
}
