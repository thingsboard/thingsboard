package org.thingsboard.server.common.transport.quota.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.KeyBasedIntervalRegistry;

@Component
public class TenantMsgsIntervalRegistry extends KeyBasedIntervalRegistry {

    public TenantMsgsIntervalRegistry(@Value("${quota.rule.tenant.intervalMs}") long intervalDurationMs,
                                      @Value("${quota.rule.tenant.ttlMs}") long ttlMs,
                                      @Value("${quota.rule.tenant.whitelist}") String whiteList,
                                      @Value("${quota.rule.tenant.blacklist}") String blackList) {
        super(intervalDurationMs, ttlMs, whiteList, blackList, "Rule Tenant");
    }
}
