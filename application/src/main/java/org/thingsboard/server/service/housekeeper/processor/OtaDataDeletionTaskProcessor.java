/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.OtaDataDeletionHousekeeperTask;
import org.thingsboard.server.dao.ota.OtaPackageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtaDataDeletionTaskProcessor extends HousekeeperTaskProcessor<OtaDataDeletionHousekeeperTask> {

    private final OtaPackageService otaPackageService;

    @Override
    public void process(OtaDataDeletionHousekeeperTask task) throws Exception {
        Long oid = task.getOid();
        if (oid == null || oid == 0) {
            log.debug("[{}][{}] Skipping OTA data deletion - no OID provided", task.getTenantId(), task.getEntityId());
            return;
        }

        Integer result = otaPackageService.unlinkLargeObject(task.getTenantId(), oid);
        if (result == null || result == -1) {
            String errorMsg = String.format("Failed to delete large object with OID: %d. lo_unlink returned: %s", oid, result);
            log.warn("[{}][{}] {}", task.getTenantId(), task.getEntityId(), errorMsg);
            throw new RuntimeException(errorMsg);
        }
        log.debug("[{}][{}] Successfully deleted large object with OID: {}", task.getTenantId(), task.getEntityId(), oid);
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_OTA_DATA;
    }

}
