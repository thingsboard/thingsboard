package com.iot.thingmodel.db;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JooqTimeTest {

    @Test
    void convertsOffsetDateTimeToInstant() {
        Instant instant = Instant.parse("2026-10-27T03:00:00Z");
        OffsetDateTime value = instant.atOffset(ZoneOffset.UTC);

        assertEquals(instant, JooqTime.toInstant(value));
    }

    @Test
    void returnsNullForNullTime() {
        assertNull(JooqTime.toInstant(null));
    }

    @Test
    void nowUsesUtc() {
        OffsetDateTime now = JooqTime.now();

        assertNotNull(now);
        assertEquals(ZoneOffset.UTC, now.getOffset());
    }
}
