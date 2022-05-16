package org.thingsboard.server.service.sync.vc.data.request.load;

import lombok.Data;

@Data
public class EntityVersionLoadConfig {

    private boolean loadRelations;
    private boolean findExistingEntityByName;

}
