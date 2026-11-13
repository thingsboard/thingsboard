// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class RuleChainExportData extends EntityExportData<RuleChain> {

    @Override
    public EntityType getEntityType() { return EntityType.RULE_CHAIN; }

    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"ruleChainId", "version"})
    private RuleChainMetaData metaData;

    @Schema(description = "Emails of users referenced in rule node configurations, keyed by exported user id. " +
            "Used on import to find the matching user when the user id is not known to the target environment.")
    @JsonProperty(index = 4)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<UUID, String> userEmails;

}
