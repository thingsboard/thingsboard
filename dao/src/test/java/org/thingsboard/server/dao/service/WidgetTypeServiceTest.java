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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@DaoSqlTest
public class WidgetTypeServiceTest extends AbstractServiceTest {

    @Autowired
    WidgetsBundleService widgetsBundleService;

    @Autowired
    WidgetTypeService widgetTypeService;

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

    @Test
    public void testSaveWidgetType() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getFqn());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(widgetType.getTenantId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());

        savedWidgetType.setName("New Widget Type");

        widgetTypeService.saveWidgetType(savedWidgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());

        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    @Test
    public void testSaveWidgetTypeWithEmptyName() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    @Test
    public void testSaveWidgetTypeWithEmptyDescriptor() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{}", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    @Test
    public void testSaveWidgetTypeWithInvalidTenant() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    @Test
    public void testUpdateWidgetTypeTenant() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetTypeService.saveWidgetType(savedWidgetType);
            });
        } finally {
            widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        }
    }

    @Test
    public void testUpdateWidgetTypeFqn() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setFqn("some_fqn");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetTypeService.saveWidgetType(savedWidgetType);
            });
        } finally {
            widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        }
    }

    @Test
    public void testFindWidgetTypeById() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetTypeDetails foundWidgetType = widgetTypeService.findWidgetTypeDetailsById(tenantId, savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    @Test
    public void testFindWidgetTypeByTenantIdAndFqn() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = new WidgetType(widgetTypeService.saveWidgetType(widgetType));
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    @Test
    public void testDeleteWidgetType() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertNull(foundWidgetType);
    }

    @Test
    public void testFindWidgetTypesByTenantIdAndWidgetsBundleId() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i=0;i<121;i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setTenantId(tenantId);
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(widgetTypeService.saveWidgetType(widgetType)));
        }

        List<WidgetTypeId> widgetTypeIds = widgetTypes.stream().map(WidgetType::getId).collect(Collectors.toList());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);

        for (WidgetTypeId id : widgetTypeIds) {
            widgetTypeService.deleteWidgetType(tenantId, id);
        }

        loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());

        Assert.assertTrue(loadedWidgetTypes.isEmpty());

        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testDeleteAllTypesFromWidgetsBundle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setTenantId(tenantId);
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(widgetTypeService.saveWidgetType(widgetType)));
        }

        List<WidgetTypeId> widgetTypeIds = widgetTypes.stream().map(WidgetType::getId).collect(Collectors.toList());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(widgetTypes.size(), loadedWidgetTypes.size());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), Collections.emptyList());

        loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(0, loadedWidgetTypes.size());
    }

}
