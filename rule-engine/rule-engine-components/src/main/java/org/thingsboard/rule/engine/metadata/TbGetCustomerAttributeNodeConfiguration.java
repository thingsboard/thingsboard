// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetCustomerAttributeNodeConfiguration extends TbGetEntityDataNodeConfiguration {

    private boolean preserveOriginatorIfCustomer;

    @Override
    public TbGetCustomerAttributeNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetCustomerAttributeNodeConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.putIfAbsent("alarmThreshold", "threshold");
        configuration.setDataMapping(dataMapping);
        configuration.setDataToFetch(DataToFetch.ATTRIBUTES);
        configuration.setFetchTo(TbMsgSource.METADATA);
        configuration.setPreserveOriginatorIfCustomer(false);
        return configuration;
    }
}
