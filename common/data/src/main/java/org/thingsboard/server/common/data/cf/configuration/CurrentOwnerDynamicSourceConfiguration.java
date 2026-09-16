// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;

@Data
public class CurrentOwnerDynamicSourceConfiguration implements CfArgumentDynamicSourceConfiguration {

    @Override
    public CFArgumentDynamicSourceType getType() {
        return CFArgumentDynamicSourceType.CURRENT_OWNER;
    }

}
