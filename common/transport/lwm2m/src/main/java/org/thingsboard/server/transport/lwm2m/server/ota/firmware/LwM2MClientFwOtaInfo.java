// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
