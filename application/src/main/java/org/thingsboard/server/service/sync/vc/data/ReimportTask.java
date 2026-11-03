// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;

@Data
public class ReimportTask {

    private final EntityExportData data;
    private final EntityImportSettings settings;

}
