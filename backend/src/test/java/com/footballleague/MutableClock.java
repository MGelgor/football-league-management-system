package com.footballleague;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Testlerde zamanı elle ilerletmek için. */
class MutableClock extends Clock {

    private volatile Instant now;

    MutableClock(Instant now) {
        this.now = now;
    }

    void set(Instant instant) {
        now = instant;
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
