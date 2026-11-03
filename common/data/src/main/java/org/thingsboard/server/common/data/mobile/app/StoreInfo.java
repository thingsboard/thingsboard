// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInfo {

    @NoXss
    private String appId;
    @NoXss
    private String sha256CertFingerprints;
    @NoXss
    private String storeLink;

}
