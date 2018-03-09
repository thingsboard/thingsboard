/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId, "BUNDLE_ALIAS_1");
        assertEquals(3, widgetTypes.size());
    }

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId, "BUNDLE_ALIAS_1", "ALIAS3");
        UUID id = UUID.fromString("2b7e4c93-2dfe-11e7-94aa-f7f6dbfb4833");
        assertEquals(id, widgetType.getId().getId());
    }
}
