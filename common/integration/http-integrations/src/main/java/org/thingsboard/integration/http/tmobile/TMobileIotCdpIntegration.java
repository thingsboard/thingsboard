// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.tmobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.AbstractHttpIntegration;

import java.util.List;

@Slf4j
public class TMobileIotCdpIntegration extends AbstractHttpIntegration<JsonHttpIntegrationMsg> {

    @Override
    protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {

        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getMsgInBytes(), metadataTemplate);
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data", data);
            }
        }
        return fromStatus(HttpStatus.OK);
    }

    @Override
    protected String getTypeUplink(JsonHttpIntegrationMsg msg) {
        return "Uplink";
    }

}
