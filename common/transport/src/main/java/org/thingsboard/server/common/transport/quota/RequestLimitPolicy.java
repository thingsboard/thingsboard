package org.thingsboard.server.common.transport.quota;


public abstract class RequestLimitPolicy {

    private final long limit;

    public RequestLimitPolicy(long limit) {
        this.limit = limit;
    }

    public boolean isValid(long currentValue) {
        return currentValue <= limit;
    }
}
