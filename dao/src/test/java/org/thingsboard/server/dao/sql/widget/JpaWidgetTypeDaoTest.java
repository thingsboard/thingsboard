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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    List<WidgetType> widgetTypeList;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Before
    public void setUp() {
        widgetTypeList = new ArrayList<>();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            widgetTypeList.add(createAndSaveWidgetType(i));
        }
    }

    WidgetType createAndSaveWidgetType(int number) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(TenantId.SYS_TENANT_ID);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setAlias("ALIAS_" + number);
        widgetType.setBundleAlias(BUNDLE_ALIAS);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    public void deleteAllWidgetType() {
        for (WidgetType widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindByTenantIdAndBundleAlias() {
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS);
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        WidgetType result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS, "ALIAS_0");
        assertEquals(result.getId(), widgetType.getId());
    }
}
