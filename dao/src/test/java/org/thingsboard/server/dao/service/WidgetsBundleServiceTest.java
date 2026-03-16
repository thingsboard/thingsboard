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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleFilter;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DaoSqlTest
public class WidgetsBundleServiceTest extends AbstractServiceTest {

    @Autowired
    WidgetsBundleService widgetsBundleService;

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

    @Test
    public void testSaveWidgetsBundle() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My first widgets bundle");

        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(widgetsBundle.getTenantId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");

        widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testSaveWidgetsBundleWithEmptyTitle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        });
    }

    @Test
    public void testSaveWidgetsBundleWithInvalidTenant() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        });
    }

    @Test
    public void testUpdateWidgetsBundleTenant() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
            });
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    @Test
    public void testUpdateWidgetsBundleAlias() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setAlias("new_alias");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
            });
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    @Test
    public void testFindWidgetsBundleById() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, savedWidgetsBundle.getAlias());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testDeleteWidgetsBundle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNull(foundWidgetsBundle);
    }

    @Test
    public void testFindSystemWidgetsBundlesByPageLink() {

        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);
        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<235;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(19);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findSystemWidgetsBundlesByPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindSystemWidgetsBundles() {
        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<135;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindTenantWidgetsBundlesByTenantId() {

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i=0;i<127;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            widgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(11);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        pageLink = new PageLink(15);
        pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindAllWidgetsBundlesByTenantIdAndPageLink() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<177;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(14);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(18);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantId() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<277;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

}
