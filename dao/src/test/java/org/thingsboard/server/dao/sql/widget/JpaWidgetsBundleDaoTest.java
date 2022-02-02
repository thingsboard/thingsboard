/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public class JpaWidgetsBundleDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Test
    public void testFindAll() {
        createSystemWidgetBundles(7, "WB_");
        List<WidgetsBundle> widgetsBundles = widgetsBundleDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        try {
            assertEquals(7, widgetsBundles.size());
        } finally {
            for (WidgetsBundle widgetsBundle:widgetsBundles) {
                widgetsBundleDao.removeById(AbstractServiceTest.SYSTEM_TENANT_ID, widgetsBundle.getUuidId());
            }
        }
    }

    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        createSystemWidgetBundles(1, "CHECK");
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(
                AbstractServiceTest.SYSTEM_TENANT_ID.getId(), "CHECK" + 0);
        try {
            System.out.println(widgetsBundle);
            assertEquals("CHECK" + 0, widgetsBundle.getAlias());
        } finally {
            List<WidgetsBundle> allWidgets = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(AbstractServiceTest.SYSTEM_TENANT_ID.getId(),
                    new PageLink(1, 0, "CHECK" + 0)).getData();
            deleteWidgetBundles(allWidgets);
        }
    }

    @Test
    public void testFindSystemWidgetsBundles() {
        createSystemWidgetBundles(30, "WB_");
        try {
            assertEquals(30, widgetsBundleDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
            // Get first page
            PageLink pageLink = new PageLink(10, 0, "WB");
            PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
            assertEquals(10, widgetsBundles1.getData().size());
            // Get next page
            pageLink = pageLink.nextPageLink();
            PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
            assertEquals(10, widgetsBundles2.getData().size());
        } finally {
            List<WidgetsBundle> allWidgets = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(AbstractServiceTest.SYSTEM_TENANT_ID.getId(),
                    new PageLink(500, 0, "WB")).getData();
            deleteWidgetBundles(allWidgets);
        }
    }

    @Test
    public void testFindWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i= 0; i < 10; i++) {
            createWidgetBundles(3, tenantId1, "WB1_");
            createWidgetBundles(5, tenantId2, "WB2_");
            createSystemWidgetBundles(10, "WB_SYS_");
        }
        try {
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
        } finally {
            List<WidgetsBundle> allWidgets =
                    widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1,
                            new PageLink(500, 0, "WB1_")).getData();
            allWidgets.addAll(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId2,
                    new PageLink(500, 0, "WB2_")).getData());
            allWidgets.addAll(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(AbstractServiceTest.SYSTEM_TENANT_ID.getId(),
                    new PageLink(500, 0, "WB_SYS_")).getData());
            deleteWidgetBundles(allWidgets);
        }
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i= 0; i < 10; i++) {
            createWidgetBundles( 5, tenantId1,"WB1_");
            createWidgetBundles(3, tenantId2, "WB2_");
            createSystemWidgetBundles(2, "WB_SYS_");
        }
        try {
            PageLink pageLink = new PageLink(30, 0, "WB");
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
        } finally {
            List<WidgetsBundle> allWidgets =
                    widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1,
                            new PageLink(500, 0, "WB1_")).getData();
            allWidgets.addAll(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId2,
                    new PageLink(500, 0, "WB2_")).getData());
            allWidgets.addAll(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(AbstractServiceTest.SYSTEM_TENANT_ID.getId(),
                    new PageLink(500, 0, "WB_SYS_")).getData());
            deleteWidgetBundles(allWidgets);
        }
    }

    @Test
    public void testSearchTextNotFound() {
        UUID tenantId = Uuids.timeBased();
        createWidgetBundles(5, tenantId, "ABC_");
        createSystemWidgetBundles(5, "SYS_");
        try {
            PageLink textPageLink = new PageLink(30, 0, "TEXT_NOT_FOUND");
            PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId, textPageLink);
            assertEquals(0, widgetsBundles4.getData().size());
        } finally {
            List<WidgetsBundle> allWidgets =
                    widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId,
                            new PageLink(500, 0, "ABC_")).getData();
            allWidgets.addAll(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(AbstractServiceTest.SYSTEM_TENANT_ID.getId(),
                    new PageLink(500, 0, "SYS_")).getData());
            deleteWidgetBundles(allWidgets);
        }
    }

    private void createWidgetBundles(int count, UUID tenantId, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundle.setTenantId(TenantId.fromUUID(tenantId));
            widgetsBundleDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, widgetsBundle);
        }
    }

    private void createSystemWidgetBundles(int count, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setTenantId(AbstractServiceTest.SYSTEM_TENANT_ID);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundleDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, widgetsBundle);
        }
    }

    void deleteWidgetBundles(List<WidgetsBundle> widgetsBundles) {
        for (WidgetsBundle widgetsBundle : widgetsBundles) {
            widgetsBundleDao.removeById(widgetsBundle.getTenantId(), widgetsBundle.getUuidId());
        }
    }
}
