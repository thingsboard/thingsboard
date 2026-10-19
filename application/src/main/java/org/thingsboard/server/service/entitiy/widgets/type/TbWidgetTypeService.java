// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.widgets.type;

import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbWidgetTypeService extends SimpleTbEntityService<WidgetTypeDetails> {

    WidgetTypeDetails save(WidgetTypeDetails widgetTypeDetails, boolean updateExistingByFqn, SecurityUser user) throws Exception;

}
