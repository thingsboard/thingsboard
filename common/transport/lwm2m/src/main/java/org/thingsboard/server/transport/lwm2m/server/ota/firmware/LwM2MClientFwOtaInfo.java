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
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;

import static org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult.UPDATE_SUCCESSFULLY;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class LwM2MClientFwOtaInfo extends LwM2MClientOtaInfo<LwM2MFirmwareUpdateStrategy, FirmwareUpdateState, FirmwareUpdateResult> {

    private Integer deliveryMethod;

    public LwM2MClientFwOtaInfo(String endpoint, String baseUrl, LwM2MFirmwareUpdateStrategy strategy) {
        super(endpoint, baseUrl, strategy);
    }

    @JsonIgnore
    @Override
    public OtaPackageType getType() {
        return OtaPackageType.FIRMWARE;
    }

    public void update(FirmwareUpdateResult result) {
        this.result = result;

        if (result.getCode() > UPDATE_SUCCESSFULLY.getCode()) {
            failedPackageId = getPackageId(targetName, targetVersion);
        }

        switch (result) {
            case INITIAL:
                break;
            case UPDATE_SUCCESSFULLY:
                retryAttempts = 0;
                break;
            default:
                break;
        }
    }

}
