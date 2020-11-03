/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.snmp.session;

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.VariableBinding;

@Slf4j
public class SnmpSessionListener implements ResponseListener {

    @Override
    public void onResponse(ResponseEvent event) {
        //TODO: Make data processing in another thread pool - parse and save attributes and telemetry
        PDU response = event.getResponse();
        if (event.getError() != null) {
            log.warn("Response error: {}", event.getError().getMessage(), event.getError());
            return;
        }

        if (response != null) {
            for (int i = 0; i < response.size(); i++) {
                //TODO: update telemetry and attributes
                VariableBinding vb = response.get(i);
                log.info("[{}] SNMP response [{}] received: {} - {}",
                        event.getPeerAddress(),
                        event.getRequest().getRequestID(),
                        vb.getOid(),
                        vb.toValueString());
            }
        } else {
            log.warn("No SNMP response, event: {}", event);
        }
    }
}
