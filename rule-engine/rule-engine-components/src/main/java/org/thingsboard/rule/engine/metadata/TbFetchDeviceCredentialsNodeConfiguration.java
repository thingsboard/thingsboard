// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbFetchDeviceCredentialsNodeConfiguration extends TbAbstractFetchToNodeConfiguration implements NodeConfiguration<TbFetchDeviceCredentialsNodeConfiguration> {

    @Override
    public TbFetchDeviceCredentialsNodeConfiguration defaultConfiguration() {
        var configuration = new TbFetchDeviceCredentialsNodeConfiguration();
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }

}
