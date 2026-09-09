// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetAttributesNodeConfiguration extends TbAbstractFetchToNodeConfiguration implements NodeConfiguration<TbGetAttributesNodeConfiguration> {

    private List<String> clientAttributeNames;
    private List<String> sharedAttributeNames;
    private List<String> serverAttributeNames;

    private List<String> latestTsKeyNames;

    private boolean tellFailureIfAbsent;
    private boolean getLatestValueWithTs;

    @Override
    public TbGetAttributesNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetAttributesNodeConfiguration();
        configuration.setClientAttributeNames(Collections.emptyList());
        configuration.setSharedAttributeNames(Collections.emptyList());
        configuration.setServerAttributeNames(Collections.emptyList());
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setTellFailureIfAbsent(true);
        configuration.setGetLatestValueWithTs(false);
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }

}
