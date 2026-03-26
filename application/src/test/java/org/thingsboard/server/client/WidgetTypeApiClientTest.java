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
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.model.PageDataWidgetTypeInfo;
import org.thingsboard.client.model.WidgetTypeDetails;
import org.thingsboard.client.model.WidgetTypeInfo;
import org.thingsboard.client.model.WidgetsBundle;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class WidgetTypeApiClientTest extends AbstractApiClientTest {

    private JsonNode createDescriptor(String type) {
        return OBJECT_MAPPER.createObjectNode()
                .put("type", type)
                .put("sizeX", 7.5)
                .put("sizeY", 5)
                .put("resources", "[]")
                .put("templateHtml", "<div class='test-widget'>Test</div>")
                .put("templateCss", ".test-widget { font-size: 14px; }")
                .put("controllerScript", "self.onInit = function() {};")
                .put("settingsSchema", "{}")
                .put("dataKeySettingsSchema", "{}");
    }

    @Test
    public void testWidgetTypeLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<WidgetTypeDetails> createdWidgetTypes = new ArrayList<>();

        // create a widgets bundle
        WidgetsBundle bundle = new WidgetsBundle(null, null, null,
                TEST_PREFIX + "Bundle_" + timestamp, null, false,
                "Test bundle description", null, null);
        WidgetsBundle savedBundle = client.saveWidgetsBundle(bundle);
        assertNotNull(savedBundle);
        assertNotNull(savedBundle.getId());
        assertEquals(bundle.getTitle(), savedBundle.getTitle());

        // create 5 widget types
        for (int i = 0; i < 5; i++) {
            String name = TEST_PREFIX + "Widget_" + timestamp + "_" + i;
            JsonNode descriptor = createDescriptor("latest");

            WidgetTypeDetails widgetType = new WidgetTypeDetails(null, null, null, name, descriptor);
            widgetType.setDescription("Test widget " + i);
            widgetType.setDeprecated(false);
            widgetType.setTags(List.of("test", "automated"));

            WidgetTypeDetails created = client.saveWidgetType(widgetType, false);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(name, created.getName());
            assertNotNull(created.getFqn());

            createdWidgetTypes.add(created);
        }

        // list widget types with text search (tenant only)
        PageDataWidgetTypeInfo filteredTypes = client.getWidgetTypes(100, 0,
                TEST_PREFIX + "Widget_" + timestamp, null, null,
                true, false, null, null, null);
        assertNotNull(filteredTypes);
        assertEquals(5, filteredTypes.getData().size());

        // get widget type details by id
        WidgetTypeDetails searchWidget = createdWidgetTypes.get(2);
        WidgetTypeDetails fetchedDetails = client.getWidgetTypeById(
                searchWidget.getId().getId().toString(), true);
        assertEquals(searchWidget.getName(), fetchedDetails.getName());
        assertEquals(searchWidget.getFqn(), fetchedDetails.getFqn());
        assertEquals("Test widget 2", fetchedDetails.getDescription());

        // get widget type info by id
        WidgetTypeInfo fetchedInfo = client.getWidgetTypeInfoById(
                searchWidget.getId().getId().toString());
        assertEquals(searchWidget.getName(), fetchedInfo.getName());

        // add widget types to bundle
        List<String> widgetTypeIds = createdWidgetTypes.stream()
                .map(wt -> wt.getId().getId().toString())
                .collect(Collectors.toList());
        client.updateWidgetsBundleWidgetTypes(savedBundle.getId().getId().toString(), widgetTypeIds);

        // get bundle widget type fqns
        List<String> bundleFqns = client.getBundleWidgetTypeFqns(savedBundle.getId().getId().toString());
        assertEquals(5, bundleFqns.size());

        // get bundle widget types details
        List<WidgetTypeDetails> bundleDetails = client.getBundleWidgetTypesDetails(
                savedBundle.getId().getId().toString(), false);
        assertEquals(5, bundleDetails.size());

        // get bundle widget types infos (paginated)
        PageDataWidgetTypeInfo bundleInfos = client.getBundleWidgetTypesInfos(
                savedBundle.getId().getId().toString(), 100, 0,
                null, null, null, null, null, null);
        assertEquals(5, bundleInfos.getData().size());

        // update widget type
        WidgetTypeDetails widgetToUpdate = client.getWidgetTypeById(
                createdWidgetTypes.get(3).getId().getId().toString(), true);
        widgetToUpdate.setDescription("Updated description");
        widgetToUpdate.setDeprecated(true);
        widgetToUpdate.setTags(List.of("test", "updated"));
        WidgetTypeDetails updatedWidget = client.saveWidgetType(widgetToUpdate, false);
        assertEquals("Updated description", updatedWidget.getDescription());
        assertEquals(true, updatedWidget.getDeprecated());

        // delete widget type
        String widgetToDeleteId = createdWidgetTypes.get(0).getId().getId().toString();
        client.deleteWidgetType(widgetToDeleteId);

        // verify deletion
        assertReturns404(() ->
                client.getWidgetTypeById(widgetToDeleteId, false)
        );

        PageDataWidgetTypeInfo typesAfterDelete = client.getWidgetTypes(100, 0,
                TEST_PREFIX + "Widget_" + timestamp, null, null,
                true, false, null, null, null);
        assertEquals(4, typesAfterDelete.getData().size());

        // delete widgets bundle
        client.deleteWidgetsBundle(savedBundle.getId().getId().toString());

        assertReturns404(() ->
                client.getWidgetsBundleById(savedBundle.getId().getId().toString(), false)
        );
    }

}
