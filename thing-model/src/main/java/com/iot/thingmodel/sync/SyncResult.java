package com.iot.thingmodel.sync;

import java.util.UUID;

public record SyncResult(UUID profileId, UUID assetId, UUID ruleChainId, int schemaBytes) {
}
