package com.iot.thingmodel.product;

import java.util.UUID;

public record TenantScope(UUID tenantId, boolean systemAdmin) {
}
