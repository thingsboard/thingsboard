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
        while(lineIterator.hasNext()) {
            currentLine = lineIterator.nextLine();
            if(currentLine.startsWith("COPY public.alarm")) {
                processBlock(lineIterator, EntityType.ALARM);
            } else if (currentLine.startsWith("COPY public.asset")) {
                processBlock(lineIterator, EntityType.ASSET);
            } else if (currentLine.startsWith("COPY public.customer")) {
                processBlock(lineIterator, EntityType.CUSTOMER);
            } else if (currentLine.startsWith("COPY public.dashboard")) {
                processBlock(lineIterator, EntityType.DASHBOARD);
            } else if (currentLine.startsWith("COPY public.device")) {
                processBlock(lineIterator, EntityType.DEVICE);
            } else if (currentLine.startsWith("COPY public.rule_chain")) {
                processBlock(lineIterator, EntityType.RULE_CHAIN);
            } else if (currentLine.startsWith("COPY public.rule_node")) {
                processBlock(lineIterator, EntityType.RULE_NODE);
            } else if (currentLine.startsWith("COPY public.tenant")) {
                processBlock(lineIterator, EntityType.TENANT);
            } else if (currentLine.startsWith("COPY public.tb_user")) {
                processBlock(lineIterator, EntityType.USER);
            } else if (currentLine.startsWith("COPY public.entity_view")) {
                processBlock(lineIterator, EntityType.ENTITY_VIEW);
            } else if (currentLine.startsWith("COPY public.widgets_bundle")) {
                processBlock(lineIterator, EntityType.WIDGETS_BUNDLE);
            } else if (currentLine.startsWith("COPY public.widget_type")) {
                processBlock(lineIterator, EntityType.WIDGET_TYPE);
            } else if (currentLine.startsWith("COPY public.tenant_profile")) {
                processBlock(lineIterator, EntityType.TENANT_PROFILE);
            } else if (currentLine.startsWith("COPY public.device_profile")) {
                processBlock(lineIterator, EntityType.DEVICE_PROFILE);
            } else if (currentLine.startsWith("COPY public.api_usage_state")) {
                processBlock(lineIterator, EntityType.API_USAGE_STATE);
            }
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
