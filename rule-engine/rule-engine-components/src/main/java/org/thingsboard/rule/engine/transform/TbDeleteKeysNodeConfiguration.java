// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Collections;
import java.util.Set;

@Data
public class TbDeleteKeysNodeConfiguration implements NodeConfiguration<TbDeleteKeysNodeConfiguration> {

    private TbMsgSource deleteFrom;
    private Set<String> keys;

    @Override
    public TbDeleteKeysNodeConfiguration defaultConfiguration() {
        TbDeleteKeysNodeConfiguration configuration = new TbDeleteKeysNodeConfiguration();
        configuration.setKeys(Collections.emptySet());
        configuration.setDeleteFrom(TbMsgSource.DATA);
        return configuration;
    }

}
