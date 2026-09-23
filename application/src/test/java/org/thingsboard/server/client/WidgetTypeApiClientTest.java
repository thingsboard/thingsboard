// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteWidgetTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteWidgetsBundleArgs;
import org.thingsboard.client.api.ThingsboardApi.GetBundleWidgetTypeFqnsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetBundleWidgetTypesDetailsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetBundleWidgetTypesInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetWidgetTypeByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetWidgetTypeInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetWidgetTypesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetWidgetsBundleByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveWidgetTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveWidgetsBundleArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateWidgetsBundleWidgetTypesArgs;
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
        WidgetsBundle savedBundle = client.saveWidgetsBundle(SaveWidgetsBundleArgs.builder()
                .widgetsBundle(bundle)
                .build());
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

            WidgetTypeDetails created = client.saveWidgetType(SaveWidgetTypeArgs.builder()
                    .widgetTypeDetails(widgetType)
                    .updateExistingByFqn(false)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(name, created.getName());
            assertNotNull(created.getFqn());

            createdWidgetTypes.add(created);
        }

        // list widget types with text search (tenant only)
        PageDataWidgetTypeInfo filteredTypes = client.getWidgetTypes(GetWidgetTypesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Widget_" + timestamp)
                .tenantOnly(true)
                .fullSearch(false)
                .build());
        assertNotNull(filteredTypes);
        assertEquals(5, filteredTypes.getData().size());

        // get widget type details by id
        WidgetTypeDetails searchWidget = createdWidgetTypes.get(2);
        WidgetTypeDetails fetchedDetails = client.getWidgetTypeById(GetWidgetTypeByIdArgs.builder()
                .widgetTypeId(searchWidget.getId().getId().toString())
                .includeResources(true)
                .build());
        assertEquals(searchWidget.getName(), fetchedDetails.getName());
        assertEquals(searchWidget.getFqn(), fetchedDetails.getFqn());
        assertEquals("Test widget 2", fetchedDetails.getDescription());

        // get widget type info by id
        WidgetTypeInfo fetchedInfo = client.getWidgetTypeInfoById(GetWidgetTypeInfoByIdArgs.builder()
                .widgetTypeId(searchWidget.getId().getId().toString())
                .build());
        assertEquals(searchWidget.getName(), fetchedInfo.getName());

        // add widget types to bundle
        List<String> widgetTypeIds = createdWidgetTypes.stream()
                .map(wt -> wt.getId().getId().toString())
                .collect(Collectors.toList());
        client.updateWidgetsBundleWidgetTypes(UpdateWidgetsBundleWidgetTypesArgs.builder()
                .widgetsBundleId(savedBundle.getId().getId().toString())
                .requestBody(widgetTypeIds)
                .build());

        // get bundle widget type fqns
        List<String> bundleFqns = client.getBundleWidgetTypeFqns(GetBundleWidgetTypeFqnsArgs.builder()
                .widgetsBundleId(savedBundle.getId().getId().toString())
                .build());
        assertEquals(5, bundleFqns.size());

        // get bundle widget types details
        List<WidgetTypeDetails> bundleDetails = client.getBundleWidgetTypesDetails(GetBundleWidgetTypesDetailsArgs.builder()
                .widgetsBundleId(savedBundle.getId().getId().toString())
                .includeResources(false)
                .build());
        assertEquals(5, bundleDetails.size());

        // get bundle widget types infos (paginated)
        PageDataWidgetTypeInfo bundleInfos = client.getBundleWidgetTypesInfos(GetBundleWidgetTypesInfosArgs.builder()
                .widgetsBundleId(savedBundle.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(5, bundleInfos.getData().size());

        // update widget type
        WidgetTypeDetails widgetToUpdate = client.getWidgetTypeById(GetWidgetTypeByIdArgs.builder()
                .widgetTypeId(createdWidgetTypes.get(3).getId().getId().toString())
                .includeResources(true)
                .build());
        widgetToUpdate.setDescription("Updated description");
        widgetToUpdate.setDeprecated(true);
        widgetToUpdate.setTags(List.of("test", "updated"));
        WidgetTypeDetails updatedWidget = client.saveWidgetType(SaveWidgetTypeArgs.builder()
                .widgetTypeDetails(widgetToUpdate)
                .updateExistingByFqn(false)
                .build());
        assertEquals("Updated description", updatedWidget.getDescription());
        assertEquals(true, updatedWidget.getDeprecated());

        // delete widget type
        String widgetToDeleteId = createdWidgetTypes.get(0).getId().getId().toString();
        client.deleteWidgetType(DeleteWidgetTypeArgs.builder()
                .widgetTypeId(widgetToDeleteId)
                .build());

        // verify deletion
        assertReturns404(() ->
                client.getWidgetTypeById(GetWidgetTypeByIdArgs.builder()
                        .widgetTypeId(widgetToDeleteId)
                        .includeResources(false)
                        .build())
        );

        PageDataWidgetTypeInfo typesAfterDelete = client.getWidgetTypes(GetWidgetTypesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Widget_" + timestamp)
                .tenantOnly(true)
                .fullSearch(false)
                .build());
        assertEquals(4, typesAfterDelete.getData().size());

        // delete widgets bundle
        client.deleteWidgetsBundle(DeleteWidgetsBundleArgs.builder()
                .widgetsBundleId(savedBundle.getId().getId().toString())
                .build());

        assertReturns404(() ->
                client.getWidgetsBundleById(GetWidgetsBundleByIdArgs.builder()
                        .widgetsBundleId(savedBundle.getId().getId().toString())
                        .inlineImages(false)
                        .build())
        );
    }

}
