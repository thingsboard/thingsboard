// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;

@Data
public class AttributesOutput implements Output {

    private String name;
    private AttributeScope scope;
    private Integer decimalsByDefault;

    private AttributesOutputStrategy strategy;

    public AttributesOutput() {
        this.strategy = new AttributesRuleChainOutputStrategy();
    }

    @Override
    public OutputType getType() {
        return OutputType.ATTRIBUTES;
    }
}
