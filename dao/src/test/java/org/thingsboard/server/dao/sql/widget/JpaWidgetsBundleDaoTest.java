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
package org.thingsboard.server.dao.sql.widget;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public class JpaWidgetsBundleDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Test
    @DatabaseSetup(value = "classpath:dbunit/widgets_bundle.xml",type= DatabaseOperation.CLEAN_INSERT)
    @DatabaseTearDown(value = "classpath:dbunit/widgets_bundle.xml", type= DatabaseOperation.DELETE_ALL)
    public void testFindAll() {
        assertEquals(7, widgetsBundleDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/widgets_bundle.xml",type= DatabaseOperation.CLEAN_INSERT)
    @DatabaseTearDown(value = "classpath:dbunit/widgets_bundle.xml", type= DatabaseOperation.DELETE_ALL)
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(
                UUID.fromString("250aca8e-2825-11e7-93ae-92361f002671"), "WB3");
        assertEquals("44e6af4e-2825-11e7-93ae-92361f002671", widgetsBundle.getId().toString());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/widgets_bundle.xml", type= DatabaseOperation.DELETE_ALL)
    public void testFindSystemWidgetsBundles() {
        createSystemWidgetBundles(30, "WB_");
        assertEquals(30, widgetsBundleDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
        // Get first page
        PageLink pageLink = new PageLink(10, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(10, widgetsBundles1.getData().size());
        // Get next page
        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(10, widgetsBundles2.getData().size());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/widgets_bundle.xml", type= DatabaseOperation.DELETE_ALL)
    public void testFindWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i= 0; i < 10; i++) {
            createWidgetBundles(3, tenantId1, "WB1_");
            createWidgetBundles(5, tenantId2, "WB2_");
            createSystemWidgetBundles(10, "WB_SYS_");
        }
        assertEquals(180, widgetsBundleDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());

        PageLink pageLink1 = new PageLink(40, 0,  "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId1, pageLink1);
        assertEquals(30, widgetsBundles1.getData().size());

        PageLink pageLink2 = new PageLink(40, 0,  "WB");
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(40, widgetsBundles2.getData().size());

        pageLink2 = pageLink2.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(10, widgetsBundles3.getData().size());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/widgets_bundle.xml", type= DatabaseOperation.DELETE_ALL)
    public void testFindAllWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i= 0; i < 10; i++) {
            createWidgetBundles( 5, tenantId1,"WB1_");
            createWidgetBundles(3, tenantId2, "WB2_");
            createSystemWidgetBundles(2, "WB_SYS_");
        }

        PageLink pageLink = new PageLink(30, 0,  "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(30, widgetsBundles1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(30, widgetsBundles2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(10, widgetsBundles3.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    @DatabaseTearDown(value = "classpath:dbunit/empty_dataset.xml", type= DatabaseOperation.DELETE_ALL)
    public void testSearchTextNotFound() {
        UUID tenantId = Uuids.timeBased();
        createWidgetBundles(5, tenantId, "ABC_");
        createSystemWidgetBundles(5, "SYS_");

        PageLink textPageLink = new PageLink(30, 0, "TEXT_NOT_FOUND");
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId, textPageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    private void createWidgetBundles(int count, UUID tenantId, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundle.setTenantId(new TenantId(tenantId));
            widgetsBundleDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, widgetsBundle);
        }
    }
    private void createSystemWidgetBundles(int count, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setTenantId(new TenantId(NULL_UUID));
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundleDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, widgetsBundle);
        }
    }
}
