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
package org.thingsboard.server.transport.lwm2m.server.ota;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;

import java.util.Optional;

@Data
@NoArgsConstructor
public abstract class LwM2MClientOtaInfo<Strategy, State, Result> {

    private String endpoint;
    private String baseUrl;

    protected String targetName;
    protected String targetVersion;
    protected String targetTag;
    protected String targetUrl;

    //TODO: use value from device if applicable;
    protected Strategy strategy;
    protected State updateState;
    protected Result result;
    protected OtaPackageUpdateStatus status;

    protected String failedPackageId;
    protected int retryAttempts;

    protected String currentName;
    protected String currentVersion3;
    protected String currentVersion;

    public LwM2MClientOtaInfo(String endpoint, String baseUrl, Strategy strategy) {
        this.endpoint = endpoint;
        this.baseUrl = baseUrl;
        this.strategy = strategy;
    }

    public void updateTarget(String targetName, String targetVersion, Optional<String> newTargetUrl, Optional<String> newTargetTag) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.targetUrl = newTargetUrl.orElse(null);
        this.targetTag = newTargetTag.orElse(null);
    }

    @JsonIgnore
    public boolean isUpdateRequired() {
        if (StringUtils.isEmpty(targetName) || StringUtils.isEmpty(targetVersion) || !isSupported()) {
            return false;
        } else {
            String targetPackageId = getPackageId(targetName, targetVersion);
            String currentPackageId = getPackageId(currentName, currentVersion);
            if (StringUtils.isNotEmpty(failedPackageId) && failedPackageId.equals(targetPackageId)) {
                return false;
            } else {
                if (targetPackageId.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(targetTag) && targetTag.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(currentVersion3)) {
                    if (StringUtils.isNotEmpty(targetTag) && (currentVersion3.contains(targetTag) || targetTag.contains(currentVersion3))) {
                        return false;
                    }
                    return !currentVersion3.contains(targetPackageId);
                } else {
                    return true;
                }
            }
        }
    }

    @JsonIgnore
    public boolean isSupported() {
        return StringUtils.isNotEmpty(currentName) || StringUtils.isNotEmpty(currentVersion) || StringUtils.isNotEmpty(currentVersion3);
    }

    @JsonIgnore
    public boolean isAssigned() {
        return StringUtils.isNotEmpty(targetName) && StringUtils.isNotEmpty(targetVersion);
    }

    public abstract void update(Result result);

    protected static String getPackageId(String name, String version) {
        return (StringUtils.isNotEmpty(name) ? name : "") + (StringUtils.isNotEmpty(version) ? version : "");
    }

    public abstract OtaPackageType getType();

    @JsonIgnore
    public String getTargetPackageId() {
        return getPackageId(targetName, targetVersion);
    }
}
