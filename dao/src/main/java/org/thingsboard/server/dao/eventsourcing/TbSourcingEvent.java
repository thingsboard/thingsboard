package org.thingsboard.server.dao.eventsourcing;

import org.thingsboard.server.common.data.id.TenantId;

public interface TbSourcingEvent extends TbEvent {

    TenantId getTenantId();

}
