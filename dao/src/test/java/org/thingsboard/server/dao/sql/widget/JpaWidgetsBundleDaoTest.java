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
package org.thingsboard.server.dao.sql.widget;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public class JpaWidgetsBundleDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Test
    @DatabaseSetup("classpath:dbunit/widgets_bundle.xml")
    public void testFindAll() {
        assertEquals(7, widgetsBundleDao.find().size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/widgets_bundle.xml")
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(
                UUID.fromString("250aca8e-2825-11e7-93ae-92361f002671"), "WB3");
        assertEquals("44e6af4e-2825-11e7-93ae-92361f002671", widgetsBundle.getId().toString());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindSystemWidgetsBundles() {
        for (int i = 0; i < 30; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias("WB" + i);
            widgetsBundle.setTitle("WB" + i);
            widgetsBundle.setId(new WidgetsBundleId(UUIDs.timeBased()));
            widgetsBundleDao.save(widgetsBundle);
        }
        assertEquals(30, widgetsBundleDao.find().size());
        // Get first page
        TextPageLink textPageLink1 = new TextPageLink(10, "WB");
        List<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(textPageLink1);
        assertEquals(10, widgetsBundles1.size());
        for (WidgetsBundle widgetsBundle : widgetsBundles1) {
            System.out.println(widgetsBundle.getSearchText());
        }
        TextPageLink textPageLink2 = new TextPageLink(10, "WB", widgetsBundles1.get(9).getId().getId(), null);
        List<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(textPageLink2);
        assertEquals(10, widgetsBundles1.size());
        for (WidgetsBundle widgetsBundle : widgetsBundles2) {
            System.out.println(widgetsBundle.getSearchText());
        }

    }
}
