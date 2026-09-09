// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;

import java.util.Arrays;
import java.util.List;

@Data
public class TbOriginatorTypeFilterNodeConfiguration implements NodeConfiguration<TbOriginatorTypeFilterNodeConfiguration> {

    private List<EntityType> originatorTypes;

    @Override
    public TbOriginatorTypeFilterNodeConfiguration defaultConfiguration() {
        TbOriginatorTypeFilterNodeConfiguration configuration = new TbOriginatorTypeFilterNodeConfiguration();
        configuration.setOriginatorTypes(Arrays.asList(
                EntityType.DEVICE
        ));
        return configuration;
    }
}
