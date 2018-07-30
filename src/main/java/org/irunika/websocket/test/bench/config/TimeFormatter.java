package org.irunika.websocket.test.bench.config;

public class TimeFormatter {

    private final long hours;
    private final long minutes;
    private final long seconds;
    private final long milliSeconds;

    public TimeFormatter(long millis) {
        this.milliSeconds = millis % 1000;

        long remainingSeconds = (millis - this.milliSeconds) / 1000;
        this.seconds = remainingSeconds % 60;

        long remainingMinutes = (remainingSeconds - seconds) / 60;
        this.minutes = remainingMinutes % 60;

        this.hours = (remainingMinutes - this.minutes) / 60;
    }

    public long getHours() {
        return hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public long getSeconds() {
        return seconds;
    }

    public long getMilliSeconds() {
        return milliSeconds;
    }
}