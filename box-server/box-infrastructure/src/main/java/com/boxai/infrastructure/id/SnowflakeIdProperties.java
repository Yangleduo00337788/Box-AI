package com.boxai.infrastructure.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "box.id")
public class SnowflakeIdProperties {

    private int workerId = 1;
    private int datacenterId = 1;

    public int getWorkerId() {
        return workerId;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public int getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(int datacenterId) {
        this.datacenterId = datacenterId;
    }
}
