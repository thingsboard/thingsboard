/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
