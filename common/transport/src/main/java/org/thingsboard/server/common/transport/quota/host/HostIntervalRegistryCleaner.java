package org.thingsboard.server.common.transport.quota.host;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryCleaner;

@Component
public class HostIntervalRegistryCleaner extends IntervalRegistryCleaner {

    public HostIntervalRegistryCleaner(HostRequestIntervalRegistry intervalRegistry,
                                       @Value("${quota.host.cleanPeriodMs}") long cleanPeriodMs) {
        super(intervalRegistry, cleanPeriodMs);
    }
}
