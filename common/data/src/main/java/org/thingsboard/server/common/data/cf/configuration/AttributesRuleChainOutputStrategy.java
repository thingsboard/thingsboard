// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@NoArgsConstructor
public class AttributesRuleChainOutputStrategy implements AttributesOutputStrategy {

    @Override
    public OutputStrategyType getType() {
        return OutputStrategyType.RULE_CHAIN;
    }

    @Override
    public boolean hasContextOnlyChanges(OutputStrategy other) {
        return !(other instanceof AttributesRuleChainOutputStrategy);
    }

    @Override
    public boolean hasRefreshContextOnlyChanges(OutputStrategy other) {
        return false;
    }

}
