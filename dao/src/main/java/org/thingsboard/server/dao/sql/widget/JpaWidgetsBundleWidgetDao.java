/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;
import org.thingsboard.server.dao.util.SqlDao;

@Component
@SqlDao
public class JpaWidgetsBundleWidgetDao implements TenantEntityDao<WidgetsBundleWidget> {

    @Autowired
    private WidgetsBundleWidgetRepository widgetsBundleWidgetRepository;

    @Override
    public WidgetsBundleWidget save(TenantId tenantId, WidgetsBundleWidget entity) {
        return widgetsBundleWidgetRepository.save(new WidgetsBundleWidgetEntity(entity)).toData();
    }

    @Override
    public PageData<WidgetsBundleWidget> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(widgetsBundleWidgetRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ObjectType getType() {
        return ObjectType.WIDGETS_BUNDLE_WIDGET;
    }

}
