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
package org.thingsboard.server.dao.widget;

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.WidgetsBundleEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraWidgetsBundleDao extends CassandraAbstractSearchTextDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    @Override
    protected Class<WidgetsBundleEntity> getColumnFamilyClass() {
        return WidgetsBundleEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return WIDGETS_BUNDLE_COLUMN_FAMILY_NAME;
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        log.debug("Try to find widgets bundle by tenantId [{}] and alias [{}]", tenantId, alias);
        Select.Where query = select().from(WIDGETS_BUNDLE_BY_TENANT_AND_ALIAS_COLUMN_FAMILY_NAME)
                .where()
                .and(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, tenantId))
                .and(eq(WIDGETS_BUNDLE_ALIAS_PROPERTY, alias));
        log.trace("Execute query {}", query);
        WidgetsBundleEntity widgetsBundleEntity = findOneByStatement(query);
        log.trace("Found widgets bundle [{}] by tenantId [{}] and alias [{}]",
                widgetsBundleEntity, tenantId, alias);
        return DaoUtil.getData(widgetsBundleEntity);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TextPageLink pageLink) {
        log.debug("Try to find system widgets bundles by pageLink [{}]", pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, NULL_UUID)),
                pageLink);
        log.trace("Found system widgets bundles [{}] by pageLink [{}]", widgetsBundlesEntities, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

    @Override
    public List<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find tenant widgets bundles by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found tenant widgets bundles [{}] by tenantId [{}] and pageLink [{}]", widgetsBundlesEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant widgets bundles by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        log.trace("Found all tenant widgets bundles [{}] by tenantId [{}] and pageLink [{}]", widgetsBundlesEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

}
