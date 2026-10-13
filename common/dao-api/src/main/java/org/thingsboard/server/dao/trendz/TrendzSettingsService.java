// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.trendz;

import org.thingsboard.server.common.data.trendz.TrendzSettings;

public interface TrendzSettingsService {

    void saveTrendzSettings(TrendzSettings settings);

    TrendzSettings findTrendzSettings();

    void deleteTrendzSettings();

}
