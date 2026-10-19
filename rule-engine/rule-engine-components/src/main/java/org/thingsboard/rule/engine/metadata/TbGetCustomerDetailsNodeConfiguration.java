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
public class TbGetCustomerDetailsNodeConfiguration extends TbAbstractGetEntityDetailsNodeConfiguration implements NodeConfiguration<TbGetCustomerDetailsNodeConfiguration> {

    @Override
    public TbGetCustomerDetailsNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetCustomerDetailsNodeConfiguration();
        configuration.setDetailsList(Collections.emptyList());
        configuration.setFetchTo(TbMsgSource.DATA);
        return configuration;
    }

}
