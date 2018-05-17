package org.thingsboard.server.common.transport.quota.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.RequestLimitPolicy;

@Component
public class TenantRequestLimitPolicy extends RequestLimitPolicy {

    public TenantRequestLimitPolicy(@Value("${quota.rule.tenant.limit}") long limit) {
        super(limit);
    }
}
