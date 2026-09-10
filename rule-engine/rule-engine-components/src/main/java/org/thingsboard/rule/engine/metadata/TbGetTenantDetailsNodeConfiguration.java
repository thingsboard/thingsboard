// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetTenantDetailsNodeConfiguration extends TbAbstractGetEntityDetailsNodeConfiguration implements NodeConfiguration<TbGetTenantDetailsNodeConfiguration> {

    @Override
    public TbGetTenantDetailsNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetTenantDetailsNodeConfiguration();
        configuration.setDetailsList(Collections.emptyList());
        configuration.setFetchTo(TbMsgSource.DATA);
        return configuration;
    }

}
