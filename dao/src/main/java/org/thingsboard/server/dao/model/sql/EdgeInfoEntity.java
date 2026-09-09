// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.edge.EdgeInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EdgeInfoEntity extends AbstractEdgeEntity<EdgeInfo> {

    public static final Map<String,String> edgeInfoColumnMap = new HashMap<>();
    static {
        edgeInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public EdgeInfoEntity() {
        super();
    }

    public EdgeInfoEntity(EdgeEntity edgeEntity,
                          String customerTitle,
                          Object customerAdditionalInfo) {
        super(edgeEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @Override
    public EdgeInfo toData() {
        return new EdgeInfo(super.toEdge(), customerTitle, customerIsPublic);
    }

}
