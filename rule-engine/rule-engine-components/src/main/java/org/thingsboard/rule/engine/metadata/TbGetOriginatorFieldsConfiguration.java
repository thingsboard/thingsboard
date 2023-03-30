/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetOriginatorFieldsConfiguration extends TbAbstractFetchToNodeConfiguration implements NodeConfiguration<TbGetOriginatorFieldsConfiguration> {

    private Map<String, String> fieldsMapping;
    private boolean ignoreNullStrings;

    @Override
    public TbGetOriginatorFieldsConfiguration defaultConfiguration() {
        var configuration = new TbGetOriginatorFieldsConfiguration();
        var fieldsMapping = new HashMap<String, String>();
        fieldsMapping.put("name", "originatorName");
        fieldsMapping.put("type", "originatorType");
        configuration.setFieldsMapping(fieldsMapping);
        configuration.setIgnoreNullStrings(false);
        configuration.setFetchTo(FetchTo.METADATA);
        return configuration;
    }

}
