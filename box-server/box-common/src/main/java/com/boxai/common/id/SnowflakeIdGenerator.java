package com.boxai.common.id;

/**
 * Twitter-style snowflake IDs that fit in signed {@code BIGINT}.
 * Layout: 41-bit timestamp | 5-bit datacenter | 5-bit worker | 12-bit sequence.
 */
public final class SnowflakeIdGenerator {

    public static final long EPOCH_MS = 1_704_067_200_000L; // 2024-01-01 UTC

    private static final long WORKER_BITS = 5L;
    private static final long DATACENTER_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
    private static final long MAX_DATACENTER = ~(-1L << DATACENTER_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private final long workerId;
    private final long datacenterId;
    private long sequence;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER) {
            throw new IllegalArgumentException("workerId must be 0-" + MAX_WORKER);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER) {
            throw new IllegalArgumentException("datacenterId must be 0-" + MAX_DATACENTER);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH_MS) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long timestamp) {
        long next = System.currentTimeMillis();
        while (next <= timestamp) {
            next = System.currentTimeMillis();
        }
        return next;
    }
}
