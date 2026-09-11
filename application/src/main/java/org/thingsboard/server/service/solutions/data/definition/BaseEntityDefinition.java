// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntityDefinition implements EntityDefinition {

    @JsonProperty("name")
    private String name;
    @JsonProperty("attributes")
    private JsonNode attributes;
    @JsonProperty("sharedAttributes")
    private JsonNode sharedAttributes;
    @JsonProperty("relations")
    private List<RelationDefinition> relations = Collections.emptyList();
    @JsonProperty("jsonId")
    private String jsonId;

    public void setRelations(List<RelationDefinition> relations) {
        if (relations != null) {
            this.relations = relations;
        }
    }
}
