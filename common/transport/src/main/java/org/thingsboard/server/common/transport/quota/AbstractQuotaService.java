package org.thingsboard.server.common.transport.quota;

import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryCleaner;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;
import org.thingsboard.server.common.transport.quota.inmemory.KeyBasedIntervalRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class AbstractQuotaService implements QuotaService {

    private final KeyBasedIntervalRegistry requestRegistry;
    private final RequestLimitPolicy requestsPolicy;
    private final IntervalRegistryCleaner registryCleaner;
    private final IntervalRegistryLogger registryLogger;
    private final boolean enabled;

    public AbstractQuotaService(KeyBasedIntervalRegistry requestRegistry, RequestLimitPolicy requestsPolicy,
                                    IntervalRegistryCleaner registryCleaner, IntervalRegistryLogger registryLogger,
                                    boolean enabled) {
        this.requestRegistry = requestRegistry;
        this.requestsPolicy = requestsPolicy;
        this.registryCleaner = registryCleaner;
        this.registryLogger = registryLogger;
        this.enabled = enabled;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            registryCleaner.schedule();
            registryLogger.schedule();
        }
    }

    @PreDestroy
    public void close() {
        if (enabled) {
            registryCleaner.stop();
            registryLogger.stop();
        }
    }

    @Override
    public boolean isQuotaExceeded(String key) {
        if (enabled) {
            long count = requestRegistry.tick(key);
            return !requestsPolicy.isValid(count);
        }
        return false;
    }
}
