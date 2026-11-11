// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import lombok.Data;

@Data
public class EmptyNodeConfiguration implements NodeConfiguration<EmptyNodeConfiguration> {

    private int version;

    @Override
    public EmptyNodeConfiguration defaultConfiguration() {
        return new EmptyNodeConfiguration();
    }
}
