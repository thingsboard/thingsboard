package org.thingsboard.server.service.sync.vc.data.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeVersionLoadConfig extends EntityVersionLoadConfig {

    private boolean removeOtherEntities;

}
