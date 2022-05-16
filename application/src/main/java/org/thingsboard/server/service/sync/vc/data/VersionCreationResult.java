package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;

@Data
public class VersionCreationResult {
    private EntityVersion version;
    private int added;
    private int modified;
    private int removed;
}
