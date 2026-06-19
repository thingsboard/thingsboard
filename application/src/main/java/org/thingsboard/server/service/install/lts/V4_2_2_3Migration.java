/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_2_2_3Migration implements LtsMigration {

    // Match on the bundle ALIAS, not the JSON filename stem.
    // The "industrial_widgets" bundle shipped in a misspelled file (industial_widgets.json),
    // but the alias inside it is correctly spelled.
    private static final List<String> OBSOLETE_BUNDLE_ALIASES = List.of(
            "air_quality", "indoor_environment", "industrial_widgets", "outdoor_environment");

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    @Override
    public String getVersion() {
        return "4.2.2.3";
    }

    @Override
    public void apply() {
        for (String alias : OBSOLETE_BUNDLE_ALIASES) {
            deprecateTypesAndDeleteBundle(alias);
        }
    }

    // Marks the bundle's widget types as deprecated (keeping the type entities) and deletes ONLY the bundle entity.
    private void deprecateTypesAndDeleteBundle(String alias) {
        WidgetsBundle bundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, alias);
        if (bundle == null) {
            return; // already removed — idempotent
        }
        List<WidgetTypeDetails> types = widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(TenantId.SYS_TENANT_ID, bundle.getId());
        for (WidgetTypeDetails type : types) {
            if (!type.isDeprecated()) {
                type.setDeprecated(true);
                widgetTypeService.saveWidgetType(type);
            }
        }
        widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, bundle.getId());
        log.info("Deprecated {} widget type(s) and removed obsolete system widget bundle: {}", types.size(), alias);
    }
}
