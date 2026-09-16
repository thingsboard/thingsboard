// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbJsonPathNodeConfiguration implements NodeConfiguration<TbJsonPathNodeConfiguration> {

    static final String DEFAULT_JSON_PATH = "$";
    private String jsonPath;

    @Override
    public TbJsonPathNodeConfiguration defaultConfiguration() {
        TbJsonPathNodeConfiguration configuration = new TbJsonPathNodeConfiguration();
        configuration.setJsonPath(DEFAULT_JSON_PATH);
        return configuration;
    }

}
