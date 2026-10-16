// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;

@Data
@EqualsAndHashCode(callSuper = true)
public class WidgetTypeExportData extends EntityExportData<WidgetTypeDetails> {

}
