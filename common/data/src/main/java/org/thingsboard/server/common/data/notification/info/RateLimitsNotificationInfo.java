package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitsNotificationInfo implements RuleOriginatedNotificationInfo {

    private TenantId tenantId;
    private String api;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
            "api", api
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return RuleOriginatedNotificationInfo.super.getAffectedTenantId();
    }

}
