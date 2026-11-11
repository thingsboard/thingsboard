// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.trendz;

import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;

public interface TrendzSyncService {

    String TRENDZ_API_KEY_DESCRIPTION = "Internal API key used to authenticate with Trendz";

    TrendzSettings performSync();

    void performSyncIfNeeded();

    TrendzHealthcheckResult performHealthcheck();

    void performApiKeyRotationSync(ApiKey newApiKey, ApiKey oldApiKey);

}
