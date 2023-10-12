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
package org.thingsboard.server.dao.sql.widget;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    List<WidgetTypeDetails> widgetTypeList;
    WidgetsBundle widgetsBundle;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Before
    public void setUp() {
        widgetTypeList = new ArrayList<>();

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setAlias(BUNDLE_ALIAS);
        widgetsBundle.setTitle(BUNDLE_ALIAS);
        widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
        widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
        this.widgetsBundle = widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);

        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            var widgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, i);
            widgetTypeList.add(widgetType);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), widgetType.getId(), i));
        }
        widgetTypeList.sort(Comparator.comparing(BaseWidgetType::getName));
    }

    WidgetTypeDetails createAndSaveWidgetType(TenantId tenantId, int number) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + number);
        widgetType.setFqn("FQN_" + number);
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", number % 2 == 0 ? "latest" : "static");
        widgetType.setDescriptor(descriptor);
        String[] tags = new String[]{"Tag1_"+number, "Tag2_"+number, "TEST_"+number};
        widgetType.setTags(tags);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    public void tearDown() {
        widgetsBundleDao.removeById(TenantId.SYS_TENANT_ID, widgetsBundle.getUuidId());
        for (WidgetType widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindByWidgetsBundleId() {
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId());
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    public void testFindSystemWidgetTypes() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, Collections.singletonList("static"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("name")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(1)), widgetTypes.getData().get(0));

        widgetTypes = widgetTypeDao.findSystemWidgetTypes(TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, Collections.emptyList(),
                new PageLink(1024, 0, "hfgfd tag2_2 ghg", new SortOrder("name")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(2)), widgetTypes.getData().get(0));
    }

    @Test
    public void testFindTenantWidgetTypesByTenantId() {
        UUID tenantId = Uuids.timeBased();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            var widgetType = createAndSaveWidgetType(new TenantId(tenantId), i);
            widgetTypeList.add(widgetType);
        }
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(tenantId, true, DeprecatedFilter.ALL, null,
                new PageLink(10, 0, "", new SortOrder("name")));
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(3)), widgetTypes.getData().get(0));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(4)), widgetTypes.getData().get(1));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(5)), widgetTypes.getData().get(2));
    }

    @Test
    public void testFindByWidgetTypeInfosByBundleId() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(),true, DeprecatedFilter.ALL, Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("name")));
        assertEquals(2, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(0)), widgetTypes.getData().get(0));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(2)), widgetTypes.getData().get(1));

        widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, Collections.emptyList(),
                new PageLink(1024, 0, "hfgfd TEST_0 ghg", new SortOrder("name")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(0)), widgetTypes.getData().get(0));
    }

    @Test
    public void testFindByTenantIdAndFqn() {
        WidgetType result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdAndFqn(TenantId.SYS_TENANT_ID.getId(), "FQN_0");
        assertEquals(result.getId(), widgetType.getId());
    }
}
