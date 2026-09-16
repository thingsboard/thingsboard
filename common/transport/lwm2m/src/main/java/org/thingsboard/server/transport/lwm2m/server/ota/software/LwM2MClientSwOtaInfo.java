// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.ota.software;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;

import static org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult.NOT_ENOUGH_STORAGE;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class LwM2MClientSwOtaInfo extends LwM2MClientOtaInfo<LwM2MSoftwareUpdateStrategy, SoftwareUpdateState, SoftwareUpdateResult> {

    public LwM2MClientSwOtaInfo(String endpoint, String baseUrl, LwM2MSoftwareUpdateStrategy strategy) {
        super(endpoint, baseUrl, strategy);
    }

    @JsonIgnore
    @Override
    public OtaPackageType getType() {
        return OtaPackageType.SOFTWARE;
    }


    public void update(SoftwareUpdateResult result) {
        this.result = result;

        if (result.getCode() >= NOT_ENOUGH_STORAGE.getCode()) {
            failedPackageId = getPackageId(targetName, targetVersion);
        }
        switch (result) {
            case INITIAL:
                break;
            case SUCCESSFULLY_INSTALLED:
                retryAttempts = 0;
                break;
            default:
                break;
        }
    }

}
