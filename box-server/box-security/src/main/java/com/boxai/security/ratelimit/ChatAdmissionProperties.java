package com.boxai.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.chat-admission")
public class ChatAdmissionProperties {

    /**
     * Global in-flight LLM cap plus wait queue. Keep true in production.
     */
    private boolean enabled = true;

    /** Concurrent model calls allowed on this deployment (all instances share Redis). */
    private int maxInflight = 200;

    /** Extra callers waiting for a slot. Beyond this, reject immediately. */
    private int maxQueue = 800;

    /** How long a waiter may sit in the queue before a busy error. */
    private int queueWaitSeconds = 60;
}
