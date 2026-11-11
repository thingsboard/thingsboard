// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WhiteLabelingCacheKey implements Serializable {

    private WhiteLabelingCompositeKey key;
    private WhiteLabelingType type;
    private String domainName;

    public static WhiteLabelingCacheKey forKey(WhiteLabelingCompositeKey key) {
        return new WhiteLabelingCacheKey(key, null, null);
    }

    public static WhiteLabelingCacheKey forTypeAndDomain(WhiteLabelingType type, String domainName) {
        return new WhiteLabelingCacheKey(null, type, domainName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WhiteLabelingCacheKey{");
        if (domainName != null && type != null) {
            builder.append("domainName=").append(domainName);
            builder.append(",type=").append(type);
        } else {
            if (key.getTenantId() != null) {
                builder.append("tenantId=").append(key.getTenantId());
            }
            if (key.getCustomerId() != null) {
                builder.append(",customerId=").append(key.getCustomerId());
            }
            if (key.getType() != null) {
                builder.append(",type=").append(key.getType());
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
