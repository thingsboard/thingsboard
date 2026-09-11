// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.widget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetsBundleWidget {

    private WidgetsBundleId widgetsBundleId;
    private WidgetTypeId widgetTypeId;
    private int widgetTypeOrder;

}
