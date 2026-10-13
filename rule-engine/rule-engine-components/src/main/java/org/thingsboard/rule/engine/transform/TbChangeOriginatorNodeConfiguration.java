// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import static org.thingsboard.rule.engine.transform.OriginatorSource.CUSTOMER;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbChangeOriginatorNodeConfiguration extends TbAbstractTransformNodeConfigurationWithRelationQuery implements NodeConfiguration<TbChangeOriginatorNodeConfiguration> {

    private OriginatorSource originatorSource;
    private String entityType;
    private String entityNamePattern;
    private boolean preserveOriginatorIfCustomer;

    @Override
    public TbChangeOriginatorNodeConfiguration defaultConfiguration() {
        var configuration = new TbChangeOriginatorNodeConfiguration();
        configuration.setOriginatorSource(CUSTOMER);
        configuration.setRelationsQuery(getDefaultRelationQuery());
        configuration.setPreserveOriginatorIfCustomer(false);
        return configuration;
    }
}
