// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Collections;
import java.util.Set;

@Data
public class TbCopyKeysNodeConfiguration implements NodeConfiguration<TbCopyKeysNodeConfiguration> {

    private TbMsgSource copyFrom;
    private Set<String> keys;

    @Override
    public TbCopyKeysNodeConfiguration defaultConfiguration() {
        TbCopyKeysNodeConfiguration configuration = new TbCopyKeysNodeConfiguration();
        configuration.setKeys(Collections.emptySet());
        configuration.setCopyFrom(TbMsgSource.DATA);
        return configuration;
    }

}
