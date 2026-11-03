// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.io.Serial;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
public class WidgetBundleInfo extends EntityInfo {

    @Serial
    private static final long serialVersionUID = 2132305394634509820L;

    public WidgetBundleInfo(@JsonProperty("id") UUID uuid, @JsonProperty("name") String name) {
        super(EntityIdFactory.getByTypeAndUuid(EntityType.WIDGETS_BUNDLE, uuid), name);
    }

}
