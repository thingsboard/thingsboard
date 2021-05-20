/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RelatedEntitiesParser {
    private final Map<String, String> allEntityIdsAndTypes = new HashMap<>();
    
    private final Map<String, EntityType> tableNameAndEntityType = Map.ofEntries(
            Map.entry("COPY public.alarm ", EntityType.ALARM),
            Map.entry("COPY public.asset ", EntityType.ASSET),
            Map.entry("COPY public.customer ", EntityType.CUSTOMER),
            Map.entry("COPY public.dashboard ", EntityType.DASHBOARD),
            Map.entry("COPY public.device ", EntityType.DEVICE),
            Map.entry("COPY public.rule_chain ", EntityType.RULE_CHAIN),
            Map.entry("COPY public.rule_node ", EntityType.RULE_NODE),
            Map.entry("COPY public.tenant ", EntityType.TENANT),
            Map.entry("COPY public.tb_user ", EntityType.USER),
            Map.entry("COPY public.entity_view ", EntityType.ENTITY_VIEW),
            Map.entry("COPY public.widgets_bundle ", EntityType.WIDGETS_BUNDLE),
            Map.entry("COPY public.widget_type ", EntityType.WIDGET_TYPE),
            Map.entry("COPY public.tenant_profile ", EntityType.TENANT_PROFILE),
            Map.entry("COPY public.device_profile ", EntityType.DEVICE_PROFILE),
            Map.entry("COPY public.api_usage_state ", EntityType.API_USAGE_STATE)
    );

    public RelatedEntitiesParser(File source) throws IOException {
        processAllTables(FileUtils.lineIterator(source));
    }

    public String getEntityType(String uuid) {
        return this.allEntityIdsAndTypes.get(uuid);
    }

    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    private void processAllTables(LineIterator lineIterator) {
        String currentLine;
        try {
            while (lineIterator.hasNext()) {
                currentLine = lineIterator.nextLine();
                for(Map.Entry<String, EntityType> entry : tableNameAndEntityType.entrySet()) {
                    if(currentLine.startsWith(entry.getKey())) {
                        processBlock(lineIterator, entry.getValue());
                    }
                }
            }
        } finally {
            lineIterator.close();
        }
    }

    private void processBlock(LineIterator lineIterator, EntityType entityType) {
        String currentLine;
        while(lineIterator.hasNext()) {
            currentLine = lineIterator.nextLine();
            if(isBlockFinished(currentLine)) {
                return;
            }
            allEntityIdsAndTypes.put(currentLine.split("\t")[0], entityType.name());
        }
    }
}
