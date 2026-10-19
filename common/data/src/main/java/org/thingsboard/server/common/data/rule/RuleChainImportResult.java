// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class RuleChainImportResult {

    @JsonIgnore
    private TenantId tenantId;
    private RuleChainId ruleChainId;
    private String ruleChainName;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean updated;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;

}
