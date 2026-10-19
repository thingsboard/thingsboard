// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetCompositeKey;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetsBundleWidgetRepository extends JpaRepository<WidgetsBundleWidgetEntity, WidgetsBundleWidgetCompositeKey> {

    List<WidgetsBundleWidgetEntity> findAllByWidgetsBundleId(UUID widgetsBundleId);

}
