package org.thingsboard.server.common.transport.quota.host;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class HostIntervalRegistryLogger extends IntervalRegistryLogger {

    private final long logIntervalMin;

    public HostIntervalRegistryLogger(@Value("${quota.host.log.topSize}") int topSize,
                                      @Value("${quota.host.log.intervalMin}") long logIntervalMin,
                                      HostRequestIntervalRegistry intervalRegistry) {
        super(topSize, logIntervalMin, intervalRegistry);
        this.logIntervalMin = logIntervalMin;
    }

    protected void log(Map<String, Long> top, int uniqHosts, long requestsCount) {
        long rps = requestsCount / TimeUnit.MINUTES.toSeconds(logIntervalMin);
        StringBuilder builder = new StringBuilder("Quota Statistic : ");
        builder.append("uniqHosts : ").append(uniqHosts).append("; ");
        builder.append("requestsCount : ").append(requestsCount).append("; ");
        builder.append("RPS : ").append(rps).append(" ");
        builder.append("top -> ");
        for (Map.Entry<String, Long> host : top.entrySet()) {
            builder.append(host.getKey()).append(" : ").append(host.getValue()).append("; ");
        }

        log.info(builder.toString());
    }
}
