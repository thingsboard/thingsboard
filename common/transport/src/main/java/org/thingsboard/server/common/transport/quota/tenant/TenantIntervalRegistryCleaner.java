package org.thingsboard.server.common.transport.quota.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryCleaner;

@Component
public class TenantIntervalRegistryCleaner extends IntervalRegistryCleaner {

    public TenantIntervalRegistryCleaner(TenantMsgsIntervalRegistry intervalRegistry,
                                         @Value("${quota.rule.tenant.cleanPeriodMs}") long cleanPeriodMs) {
        super(intervalRegistry, cleanPeriodMs);
    }
}
