// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class RuleChainExportData extends EntityExportData<RuleChain> {

    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"ruleChainId", "version"})
    private RuleChainMetaData metaData;

}
