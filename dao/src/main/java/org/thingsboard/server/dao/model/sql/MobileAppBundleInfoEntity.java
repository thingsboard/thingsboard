/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class MobileAppBundleInfoEntity extends AbstractMobileAppBundleEntity<MobileAppBundleInfo> {

    private String androidPkgName;
    private String iosPkgName;
    private boolean qrCodeEnabled;

    public MobileAppBundleInfoEntity() {
        super();
    }

    public MobileAppBundleInfoEntity(MobileAppBundleEntity mobileAppBundleEntity, String androidPkgName, String iosPkgName, boolean qrCodeEnabled) {
        super(mobileAppBundleEntity);
        this.androidPkgName = androidPkgName;
        this.iosPkgName = iosPkgName;
        this.qrCodeEnabled = qrCodeEnabled;
    }

    @Override
    public MobileAppBundleInfo toData() {
        return new MobileAppBundleInfo(super.toMobileAppBundle(), androidPkgName, iosPkgName, qrCodeEnabled);
    }
}
