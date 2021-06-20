/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.ota;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.UpdateStateFw;
import org.thingsboard.server.transport.lwm2m.server.UpdateResultFw;

import java.util.Optional;

@Data
public class LwM2MClientOtaInfo {

    private final String endpoint;
    private final OtaPackageType type;

    private String baseUrl;

    private boolean targetFetchFailure;
    private String targetName;
    private String targetVersion;
    private String targetUrl;

    private boolean currentFetchFailure;
    private String currentName;
    private String currentVersion3;
    private String currentVersion5;
    private Integer deliveryMethod;

    //TODO: use value from device if applicable;
    private LwM2MFirmwareUpdateStrategy strategy;
    private UpdateStateFw updateState;
    private UpdateResultFw updateResult;

    private String failedPackageId;
    private int retryAttempts;

    public LwM2MClientOtaInfo(String endpoint, OtaPackageType type, Integer strategyCode, String baseUrl) {
        this.endpoint = endpoint;
        this.type = type;
        this.strategy = LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(strategyCode);
        this.baseUrl = baseUrl;
    }

    public void updateTarget(String targetName, String targetVersion, Optional<String> newFirmwareUrl) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.targetUrl = newFirmwareUrl.orElse(null);
    }

    public boolean isUpdateRequired() {
        if (StringUtils.isEmpty(targetName) || StringUtils.isEmpty(targetVersion) || !isSupported()) {
            return false;
        } else {
            String targetPackageId = getPackageId(targetName, targetVersion);
            String currentPackageIdUsingObject5 = getPackageId(currentName, currentVersion5);
            if (StringUtils.isNotEmpty(failedPackageId) && failedPackageId.equals(targetPackageId)) {
                return false;
            } else {
                if (targetPackageId.equals(currentPackageIdUsingObject5)) {
                    return false;
                } else if (StringUtils.isNotEmpty(currentVersion3)) {
                    return !currentVersion3.contains(targetPackageId);
                } else {
                    return true;
                }
            }
        }
    }

    public boolean isSupported() {
        return StringUtils.isNotEmpty(currentName) || StringUtils.isNotEmpty(currentVersion5) || StringUtils.isNotEmpty(currentVersion3);
    }

    public void setUpdateResult(UpdateResultFw updateResult) {
        this.updateResult = updateResult;
        switch (updateResult) {
            case INITIAL:
                break;
            case UPDATE_SUCCESSFULLY:
                retryAttempts = 0;
                break;
            default:
                failedPackageId = getPackageId(targetName, targetVersion);
                break;
        }
    }

    private static String getPackageId(String name, String version) {
        return (StringUtils.isNotEmpty(name) ? name : "") + (StringUtils.isNotEmpty(version) ? version : "");
    }

}
