/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseWidgetTypeServiceTest extends AbstractServiceTest {

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveWidgetType() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);


        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getAlias());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(widgetType.getTenantId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());
        Assert.assertEquals(savedWidgetsBundle.getAlias(), savedWidgetType.getBundleAlias());

        savedWidgetType.setName("New Widget Type");

        widgetTypeService.saveWidgetType(savedWidgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(savedWidgetType.getId());
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());

        widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetTypeWithEmptyName() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        try {
            widgetTypeService.saveWidgetType(widgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetTypeWithEmptyBundleAlias() throws IOException {
        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        widgetTypeService.saveWidgetType(widgetType);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetTypeWithEmptyDescriptor() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setDescriptor(new ObjectMapper().readValue("{}", JsonNode.class));
        try {
            widgetTypeService.saveWidgetType(widgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetTypeWithInvalidTenant() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(new TenantId(UUIDs.timeBased()));
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        try {
            widgetTypeService.saveWidgetType(widgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetTypeWithInvalidBundleAlias() throws IOException {
        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias("some_alias");
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        widgetTypeService.saveWidgetType(widgetType);
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateWidgetTypeTenant() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setTenantId(new TenantId(ModelConstants.NULL_UUID));
        try {
            widgetTypeService.saveWidgetType(savedWidgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateWidgetTypeBundleAlias() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setBundleAlias("some_alias");
        try {
            widgetTypeService.saveWidgetType(savedWidgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateWidgetTypeAlias() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setAlias("some_alias");
        try {
            widgetTypeService.saveWidgetType(savedWidgetType);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
        }
    }

    @Test
    public void testFindWidgetTypeById() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);

        widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
    }

    @Test
    public void testFindWidgetTypeByTenantIdBundleAliasAndAlias() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, savedWidgetsBundle.getAlias(), savedWidgetType.getAlias());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);

        widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
    }

    @Test
    public void testDeleteWidgetType() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        widgetType.setTenantId(tenantId);
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        widgetTypeService.deleteWidgetType(savedWidgetType.getId());
        foundWidgetType = widgetTypeService.findWidgetTypeById(savedWidgetType.getId());
        Assert.assertNull(foundWidgetType);

        widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
    }

    @Test
    public void testFindWidgetTypesByTenantIdAndBundleAlias() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i=0;i<121;i++) {
            WidgetType widgetType = new WidgetType();
            widgetType.setTenantId(tenantId);
            widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(widgetTypeService.saveWidgetType(widgetType));
        }

        List<WidgetType> loadedWidgetTypes = widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, savedWidgetsBundle.getAlias());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);

        widgetTypeService.deleteWidgetTypesByTenantIdAndBundleAlias(tenantId, savedWidgetsBundle.getAlias());

        loadedWidgetTypes = widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, savedWidgetsBundle.getAlias());

        Assert.assertTrue(loadedWidgetTypes.isEmpty());

        widgetsBundleService.deleteWidgetsBundle(savedWidgetsBundle.getId());
    }

}
