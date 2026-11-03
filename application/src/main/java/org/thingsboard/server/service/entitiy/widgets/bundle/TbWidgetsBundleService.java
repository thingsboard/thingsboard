// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.widgets.bundle;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;

import java.util.List;

public interface TbWidgetsBundleService extends SimpleTbEntityService<WidgetsBundle> {

    void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds, User user) throws Exception;

    void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetFqns, User user) throws Exception;


}
