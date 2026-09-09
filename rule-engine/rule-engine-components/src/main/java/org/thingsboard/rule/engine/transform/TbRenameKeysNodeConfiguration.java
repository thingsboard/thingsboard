// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Map;

@Data
public class TbRenameKeysNodeConfiguration implements NodeConfiguration<TbRenameKeysNodeConfiguration> {

    private TbMsgSource renameIn;
    private Map<String, String> renameKeysMapping;

    @Override
    public TbRenameKeysNodeConfiguration defaultConfiguration() {
        TbRenameKeysNodeConfiguration configuration = new TbRenameKeysNodeConfiguration();
        configuration.setRenameKeysMapping(Map.of("temperatureCelsius", "temperature"));
        configuration.setRenameIn(TbMsgSource.DATA);
        return configuration;
    }

}
