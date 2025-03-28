package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;

@ToString(callSuper = true)
@Data
public class DeviceSetting {
    private Device device;
    private List<AttributeKvEntry> attributes;
}
