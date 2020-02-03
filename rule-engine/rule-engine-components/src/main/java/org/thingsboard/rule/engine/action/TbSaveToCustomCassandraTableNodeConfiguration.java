/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.HashMap;
import java.util.Map;

@Data
public class TbSaveToCustomCassandraTableNodeConfiguration implements NodeConfiguration<TbSaveToCustomCassandraTableNodeConfiguration> {


    private String tableName;
    private Map<String, String> fieldsMapping;


    @Override
    public TbSaveToCustomCassandraTableNodeConfiguration defaultConfiguration() {
        TbSaveToCustomCassandraTableNodeConfiguration configuration = new TbSaveToCustomCassandraTableNodeConfiguration();
        configuration.setTableName("");
        Map<String, String> map = new HashMap<>();
        map.put("", "");
        configuration.setFieldsMapping(map);
        return configuration;
    }
}
