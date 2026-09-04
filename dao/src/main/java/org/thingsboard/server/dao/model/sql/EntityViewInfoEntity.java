// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityViewInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityViewInfoEntity extends AbstractEntityViewEntity<EntityViewInfo> {

    public static final Map<String,String> entityViewInfoColumnMap = new HashMap<>();
    static {
        entityViewInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public EntityViewInfoEntity() {
        super();
    }

    public EntityViewInfoEntity(EntityViewEntity entityViewEntity,
                                String customerTitle,
                                Object customerAdditionalInfo) {
        super(entityViewEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @Override
    public EntityViewInfo toData() {
        return new EntityViewInfo(super.toEntityView(), customerTitle, customerIsPublic);
    }
}
