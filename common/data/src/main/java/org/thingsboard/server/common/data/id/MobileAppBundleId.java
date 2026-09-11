// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class MobileAppBundleId extends UUIDBased implements EntityId{

    @JsonCreator
    public MobileAppBundleId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static MobileAppBundleId fromString(String mobileAppId) {
        return new MobileAppBundleId(UUID.fromString(mobileAppId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP_BUNDLE;
    }
}
