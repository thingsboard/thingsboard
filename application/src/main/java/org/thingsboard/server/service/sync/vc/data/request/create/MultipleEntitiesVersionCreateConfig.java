package org.thingsboard.server.service.sync.vc.data.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class MultipleEntitiesVersionCreateConfig extends VersionCreateConfig {
    private SyncStrategy syncStrategy;
}
