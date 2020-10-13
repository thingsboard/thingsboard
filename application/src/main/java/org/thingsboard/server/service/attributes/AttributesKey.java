package org.thingsboard.server.service.attributes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class AttributesKey {
    private final String scope;
    private final EntityId entityId;
    private final String key;
}
