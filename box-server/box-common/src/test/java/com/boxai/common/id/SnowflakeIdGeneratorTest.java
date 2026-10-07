package com.boxai.common.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void nextIdIsUniqueAndIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> seen = new HashSet<>();
        long previous = 0L;
        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId();
            assertTrue(id > previous);
            assertTrue(seen.add(id));
            previous = id;
        }
        assertEquals(10_000, seen.size());
    }

    @Test
    void rejectsOutOfRangeWorker() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(0, 32));
    }
}
