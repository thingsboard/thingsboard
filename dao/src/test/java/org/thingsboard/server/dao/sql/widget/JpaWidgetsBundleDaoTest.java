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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public class JpaWidgetsBundleDaoTest extends AbstractJpaDaoTest {

    List<WidgetsBundle> widgetsBundles;
    List<WidgetType> widgetTypeList;

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Before
    public void setUp() {
        widgetTypeList = new ArrayList<>();
    }

    @After
    public void tearDown() {
        for (WidgetsBundle widgetsBundle : widgetsBundles) {
            widgetsBundleDao.removeById(widgetsBundle.getTenantId(), widgetsBundle.getUuidId());
        }
        for (WidgetType widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindAll() {
        createSystemWidgetBundles(7, "WB_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(7, widgetsBundles.size());
    }

    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        createSystemWidgetBundles(1, "WB_");
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(
                TenantId.SYS_TENANT_ID.getId(), "WB_" + 0);
        widgetsBundles = List.of(widgetsBundle);
        assertEquals("WB_" + 0, widgetsBundle.getAlias());
    }

    @Test
    public void testFindSystemWidgetsBundles() {
        createSystemWidgetBundles(30, "WB_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(30, widgetsBundles.size());
        // Get first page
        PageLink pageLink = new PageLink(10, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, false, pageLink);
        assertEquals(10, widgetsBundles1.getData().size());
        // Get next page
        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, false, pageLink);
        assertEquals(10, widgetsBundles2.getData().size());
    }

    @Test
    public void testFindSystemWidgetsBundlesFullSearch() {
        createSystemWidgetBundles(30, "WB_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID).stream().sorted(Comparator.comparing(WidgetsBundle::getTitle)).collect(Collectors.toList());
        assertEquals(30, widgetsBundles.size());

        var widgetType1 = createAndSaveWidgetType(TenantId.SYS_TENANT_ID,1, "Test widget type 1", "This is the widget type 1", new String[]{"tag1", "Tag2", "TEST_TAG"});
        var widgetType2 = createAndSaveWidgetType(TenantId.SYS_TENANT_ID,2, "Test widget type 2", "This is the widget type 2", new String[]{"tag3", "Tag5", "TEST_Tag2"});

        var widgetsBundle1 = widgetsBundles.get(10);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle1.getId(), widgetType1.getId(), 0));

        var widgetsBundle2 = widgetsBundles.get(15);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle2.getId(), widgetType2.getId(), 0));


        var widgetsBundle3 = widgetsBundles.get(28);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle3.getId(), widgetType1.getId(), 0));
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle3.getId(), widgetType2.getId(), 1));

        PageLink pageLink = new PageLink(10, 0, "widget type 1", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, true, pageLink);
        assertEquals(2, widgetsBundles1.getData().size());
        assertEquals(widgetsBundle1, widgetsBundles1.getData().get(0));
        assertEquals(widgetsBundle3, widgetsBundles1.getData().get(1));

        pageLink = new PageLink(10, 0, "Test widget type 2", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, true, pageLink);
        assertEquals(2, widgetsBundles2.getData().size());
        assertEquals(widgetsBundle2, widgetsBundles2.getData().get(0));
        assertEquals(widgetsBundle3, widgetsBundles2.getData().get(1));

        pageLink = new PageLink(10, 0, "ppp Fd v TAG1 tt", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, true, pageLink);
        assertEquals(2, widgetsBundles3.getData().size());
        assertEquals(widgetsBundle1, widgetsBundles3.getData().get(0));
        assertEquals(widgetsBundle3, widgetsBundles3.getData().get(1));
    }

    @Test
    public void testFindWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i = 0; i < 10; i++) {
            createWidgetBundles(3, tenantId1, "WB1_" + i + "_");
            createWidgetBundles(5, tenantId2, "WB2_" + i + "_");
            createSystemWidgetBundles(10, "WB_SYS_" + i + "_");
        }
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(180, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink1 = new PageLink(40, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId1, pageLink1);
        assertEquals(30, widgetsBundles1.getData().size());

        PageLink pageLink2 = new PageLink(40, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(40, widgetsBundles2.getData().size());

        pageLink2 = pageLink2.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(10, widgetsBundles3.getData().size());
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i = 0; i < 10; i++) {
            createWidgetBundles(5, tenantId1, "WB1_" + i + "_");
            createWidgetBundles(3, tenantId2, "WB2_" + i + "_");
            createSystemWidgetBundles(2, "WB_SYS_" + i + "_");
        }
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(100, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink = new PageLink(30, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, false, pageLink);
        assertEquals(30, widgetsBundles1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, false, pageLink);
        assertEquals(30, widgetsBundles2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, false, pageLink);
        assertEquals(10, widgetsBundles3.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, false, pageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantIdFullSearch() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        for (int i = 0; i < 10; i++) {
            createWidgetBundles(5, tenantId1, "WB1_" + i + "_");
            createWidgetBundles(3, tenantId2, "WB2_" + i + "_");
            createSystemWidgetBundles(2, "WB_SYS_" + i + "_");
        }
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID).stream().sorted(Comparator.comparing(WidgetsBundle::getTitle)).collect(Collectors.toList());;
        assertEquals(100, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());

        var widgetType1 = createAndSaveWidgetType(new TenantId(tenantId1), 1, "Test widget type 1", "This is the widget type 1", new String[]{"tag1", "Tag2", "TEST_TAG"});
        var widgetType2 = createAndSaveWidgetType(new TenantId(tenantId2), 2, "Test widget type 2", "This is the widget type 2", new String[]{"tag3", "Tag5", "TEST_Tag2"});

        var widgetsBundle1 = widgetsBundles.stream().filter(widgetsBundle -> widgetsBundle.getTenantId().getId().equals(tenantId1)).collect(Collectors.toList()).get(10);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle1.getId(), widgetType1.getId(), 0));

        var widgetsBundle2 = widgetsBundles.stream().filter(widgetsBundle -> widgetsBundle.getTenantId().getId().equals(tenantId2)).collect(Collectors.toList()).get(15);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle2.getId(), widgetType2.getId(), 0));

        var widgetsBundle3 = widgetsBundles.stream().filter(widgetsBundle -> widgetsBundle.getTenantId().getId().equals(tenantId2)).collect(Collectors.toList()).get(28);
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle3.getId(), widgetType1.getId(), 0));
        widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(widgetsBundle3.getId(), widgetType2.getId(), 1));

        PageLink pageLink = new PageLink(10, 0, "widget type 1", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, true,  pageLink);
        assertEquals(1, widgetsBundles1.getData().size());
        assertEquals(widgetsBundle1, widgetsBundles1.getData().get(0));

        pageLink = new PageLink(10, 0, "Test widget type 2", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, true, pageLink);
        assertEquals(0, widgetsBundles2.getData().size());

        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId2, true, pageLink);
        assertEquals(2, widgetsBundles3.getData().size());
        assertEquals(widgetsBundle2, widgetsBundles3.getData().get(0));
        assertEquals(widgetsBundle3, widgetsBundles3.getData().get(1));

        pageLink = new PageLink(10, 0, "ttt Tag2 ffff hhhh", new SortOrder("title"));
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, true, pageLink);
        assertEquals(1, widgetsBundles4.getData().size());
        assertEquals(widgetsBundle1, widgetsBundles4.getData().get(0));

        PageData<WidgetsBundle> widgetsBundles5 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId2, true, pageLink);
        assertEquals(1, widgetsBundles5.getData().size());
        assertEquals(widgetsBundle3, widgetsBundles5.getData().get(0));
    }

    @Test
    public void testSearchTextNotFound() {
        UUID tenantId = Uuids.timeBased();
        createWidgetBundles(5, tenantId, "ABC_");
        createSystemWidgetBundles(5, "SYS_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(10, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());
        PageLink textPageLink = new PageLink(30, 0, "TEXT_NOT_FOUND");
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId, false, textPageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    private void createWidgetBundles(int count, UUID tenantId, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundle.setTenantId(TenantId.fromUUID(tenantId));
            widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);
        }
    }

    private void createSystemWidgetBundles(int count, String prefix) {
        for (int i = 0; i < count; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);
        }
    }

    WidgetType createAndSaveWidgetType(TenantId tenantId, int number, String name, String description, String[] tags) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName(name);
        widgetType.setDescription(description);
        widgetType.setTags(tags);
        widgetType.setFqn("FQN_" + number);
        var saved = widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
        this.widgetTypeList.add(saved);
        return saved;
    }
}
