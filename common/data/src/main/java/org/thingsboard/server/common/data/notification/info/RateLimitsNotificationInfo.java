package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitsNotificationInfo implements RuleOriginatedNotificationInfo {

    private TenantId tenantId;
    private String tenantName;
    private String api;
    private EntityId limitLevel;
    private String limitLevelEntityName;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "api", api,
                "limitLevelEntityType", limitLevel != null ? limitLevel.getEntityType().getNormalName() : null,
                "limitLevelEntityId", limitLevel != null ? limitLevel.getId().toString() : null,
                "limitLevelEntityName", limitLevelEntityName,
                "tenantName", tenantName,
                "tenantId", tenantId.toString()
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

}
