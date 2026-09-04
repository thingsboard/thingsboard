// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entityview;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;
import java.util.List;

@Getter
@EqualsAndHashCode
@Builder
public class EntityViewCacheValue implements Serializable, HasVersion {

    private static final long serialVersionUID = 1959004642076413174L;

    private final EntityView entityView;
    private final List<EntityView> entityViews;

    @Override
    public Long getVersion() {
        return entityView != null ? entityView.getVersion() : 0;
    }

}
