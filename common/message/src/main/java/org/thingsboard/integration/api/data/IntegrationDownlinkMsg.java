// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.Serializable;

/**
 * Created by ashvayka on 22.02.18.
 */
public interface IntegrationDownlinkMsg extends Serializable {

    TenantId getTenantId();

    IntegrationId getIntegrationId();

    EntityId getEntityId();

    TbMsg getTbMsg();

    String getEntityName();

}
