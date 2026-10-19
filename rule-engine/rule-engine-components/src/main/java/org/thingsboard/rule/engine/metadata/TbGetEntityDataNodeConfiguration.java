// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetEntityDataNodeConfiguration extends TbGetMappedDataNodeConfiguration implements NodeConfiguration<TbGetEntityDataNodeConfiguration> {

    private DataToFetch dataToFetch;

    @Override
    public TbGetEntityDataNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetEntityDataNodeConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.putIfAbsent("alarmThreshold", "threshold");
        configuration.setDataMapping(dataMapping);
        configuration.setDataToFetch(DataToFetch.ATTRIBUTES);
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }

}
