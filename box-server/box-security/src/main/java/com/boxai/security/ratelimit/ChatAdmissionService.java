package com.boxai.security.ratelimit;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

@Service
public class ChatAdmissionService {

    public static final String BUSY_MESSAGE = "当前对话人数较多，请稍后再试";
    public static final String QUEUE_TIMEOUT_MESSAGE = "排队等待超时，请稍后再试";

    private static final String INFLIGHT_KEY = "box:chat:inflight";
    private static final String WAIT_KEY = "box:chat:waitlist";
    private static final Duration KEY_TTL = Duration.ofHours(1);

    private static final String ENTER_LUA = """
            local inflight = tonumber(redis.call('GET', KEYS[1]) or '0')
            local maxInflight = tonumber(ARGV[1])
            if inflight < maxInflight then
              redis.call('INCR', KEYS[1])
              redis.call('EXPIRE', KEYS[1], ARGV[3])
              return {1, 0}
            end
            local waiting = redis.call('LLEN', KEYS[2])
            if waiting < tonumber(ARGV[2]) then
              redis.call('RPUSH', KEYS[2], ARGV[4])
              redis.call('EXPIRE', KEYS[2], ARGV[3])
              return {0, waiting + 1}
            end
            return {-1, waiting}
            """;

    private static final String PROMOTE_LUA = """
            local inflight = tonumber(redis.call('GET', KEYS[1]) or '0')
            local maxInflight = tonumber(ARGV[2])
            local ticket = ARGV[1]
            if inflight < maxInflight then
              local first = redis.call('LINDEX', KEYS[2], 0)
              if first == ticket then
                redis.call('LPOP', KEYS[2])
                redis.call('INCR', KEYS[1])
                redis.call('EXPIRE', KEYS[1], ARGV[3])
                return {1, 0}
              end
            end
            local pos = redis.call('LPOS', KEYS[2], ticket)
            if not pos then
              return {-1, 0}
            end
            return {0, pos + 1}
            """;

    private static final String LEAVE_LUA = """
            redis.call('LREM', KEYS[1], 1, ARGV[1])
            return 1
            """;

    private static final String RELEASE_LUA = """
            local inflight = tonumber(redis.call('GET', KEYS[1]) or '0')
            if inflight > 0 then
              redis.call('DECR', KEYS[1])
            end
            return 1
            """;

    private final RedisService redisService;
    private final ChatAdmissionProperties properties;

    public ChatAdmissionService(RedisService redisService, ChatAdmissionProperties properties) {
        this.redisService = redisService;
        this.properties = properties;
    }

    public ChatSlot acquire(IntConsumer onQueued) {
        if (properties == null || !properties.isEnabled()) {
            return ChatSlot.noop();
        }
        int maxInflight = Math.max(1, properties.getMaxInflight());
        int maxQueue = Math.max(0, properties.getMaxQueue());
        int waitSeconds = Math.max(1, properties.getQueueWaitSeconds());
        String ttl = String.valueOf(KEY_TTL.toSeconds());
        String ticket = UUID.randomUUID().toString();
        List<Object> entered = redisService.eval(
                ENTER_LUA,
                List.of(INFLIGHT_KEY, WAIT_KEY),
                String.valueOf(maxInflight),
                String.valueOf(maxQueue),
                ttl,
                ticket);
        int status = asInt(entered, 0);
        int position = asInt(entered, 1);
        if (status == 1) {
            return ChatSlot.inflight(this::releaseInflight);
        }
        if (status < 0) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, BUSY_MESSAGE);
        }
        notifyQueued(onQueued, position);
        long deadline = System.nanoTime() + Duration.ofSeconds(waitSeconds).toNanos();
        try {
            while (System.nanoTime() < deadline) {
                sleepQuietly();
                List<Object> promoted = redisService.eval(
                        PROMOTE_LUA,
                        List.of(INFLIGHT_KEY, WAIT_KEY),
                        ticket,
                        String.valueOf(maxInflight),
                        ttl);
                int promoteStatus = asInt(promoted, 0);
                int queuedAt = asInt(promoted, 1);
                if (promoteStatus == 1) {
                    return ChatSlot.inflight(this::releaseInflight);
                }
                if (promoteStatus < 0) {
                    throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, BUSY_MESSAGE);
                }
                notifyQueued(onQueued, queuedAt);
            }
            leaveQueue(ticket);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, QUEUE_TIMEOUT_MESSAGE);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            leaveQueue(ticket);
            throw ex;
        }
    }

    private void leaveQueue(String ticket) {
        redisService.evalLong(LEAVE_LUA, List.of(WAIT_KEY), ticket);
    }

    private void releaseInflight() {
        redisService.evalLong(RELEASE_LUA, List.of(INFLIGHT_KEY));
    }

    private static void notifyQueued(IntConsumer onQueued, int position) {
        if (onQueued != null && position > 0) {
            onQueued.accept(position);
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(200L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, BUSY_MESSAGE);
        }
    }

    private static int asInt(List<Object> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return 0;
        }
        Object value = values.get(index);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    public static final class ChatSlot implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final Runnable releaser;

        private ChatSlot(Runnable releaser) {
            this.releaser = releaser;
        }

        static ChatSlot noop() {
            return new ChatSlot(() -> {
            });
        }

        static ChatSlot inflight(Runnable releaser) {
            return new ChatSlot(releaser);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                releaser.run();
            }
        }
    }
}
