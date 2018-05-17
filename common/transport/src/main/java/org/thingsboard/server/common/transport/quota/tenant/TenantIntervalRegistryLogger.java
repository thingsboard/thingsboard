package org.thingsboard.server.common.transport.quota.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TenantIntervalRegistryLogger extends IntervalRegistryLogger {

    private final long logIntervalMin;

    public TenantIntervalRegistryLogger(@Value("${quota.rule.tenant.log.topSize}") int topSize,
                                        @Value("${quota.rule.tenant.log.intervalMin}") long logIntervalMin,
                                        TenantMsgsIntervalRegistry intervalRegistry) {
        super(topSize, logIntervalMin, intervalRegistry);
        this.logIntervalMin = logIntervalMin;
    }

    protected void log(Map<String, Long> top, int uniqHosts, long requestsCount) {
        long rps = requestsCount / TimeUnit.MINUTES.toSeconds(logIntervalMin);
        StringBuilder builder = new StringBuilder("Tenant Quota Statistic : ");
        builder.append("uniqTenants : ").append(uniqHosts).append("; ");
        builder.append("requestsCount : ").append(requestsCount).append("; ");
        builder.append("RPS : ").append(rps).append(" ");
        builder.append("top -> ");
        for (Map.Entry<String, Long> host : top.entrySet()) {
            builder.append(host.getKey()).append(" : ").append(host.getValue()).append("; ");
        }

        log.info(builder.toString());
    }
}
