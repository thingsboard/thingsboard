package org.thingsboard.server.common.transport.quota.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.AbstractQuotaService;

@Component
public class TenantQuotaService extends AbstractQuotaService {

    public TenantQuotaService(TenantMsgsIntervalRegistry requestRegistry, TenantRequestLimitPolicy requestsPolicy,
                              TenantIntervalRegistryCleaner registryCleaner, TenantIntervalRegistryLogger registryLogger,
                              @Value("${quota.rule.tenant.enabled}") boolean enabled) {
        super(requestRegistry, requestsPolicy, registryCleaner, registryLogger, enabled);
    }
}
