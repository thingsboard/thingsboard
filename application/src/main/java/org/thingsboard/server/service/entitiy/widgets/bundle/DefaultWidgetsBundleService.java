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
package org.thingsboard.server.service.entitiy.widgets.bundle;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetsBundleService extends AbstractTbEntityService implements TbWidgetsBundleService {

    private final WidgetsBundleService widgetsBundleService;

    @Override
    public WidgetsBundle save(WidgetsBundle widgetsBundle, User user) throws Exception {
        ActionType actionType = widgetsBundle.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            WidgetsBundle savedWidgetsBundle = checkNotNull(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
            autoCommit(user, savedWidgetsBundle.getId());
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedWidgetsBundle.getId(),
                    savedWidgetsBundle, user, actionType, true, null);
            return savedWidgetsBundle;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE), widgetsBundle, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(WidgetsBundle widgetsBundle, User user) {
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getTenantId(), widgetsBundle.getId());
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, widgetsBundle.getId(), widgetsBundle,
                    user, ActionType.DELETED, true, null);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE),
                    ActionType.DELETED, user, e, widgetsBundle.getId());
            throw e;
        }
    }
}
