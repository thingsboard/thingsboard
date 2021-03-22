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
package org.thingsboard.server.transport.snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.UdpTransportMapping;

import java.io.IOException;

/**
 * For testing purposes. Will be removed when the time comes
 */
public class SnmpDeviceSimulator {
    private final Target target;
    private final OID oid = new OID(".1.3.6.1.2.1.1.1.0");
    private Snmp snmp;

    public SnmpDeviceSimulator(int port) {
        String address = "udp:127.0.0.1/" + port;

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(GenericAddress.parse(address));
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        this.target = target;
    }

    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulator deviceSimulator = new SnmpDeviceSimulator(161);

        deviceSimulator.start();
        String response = deviceSimulator.sendRequest(PDU.GET);

        System.out.println(response);
    }

    public void start() throws IOException {
        UdpTransportMapping transport = new DefaultUdpTransportMapping();
        transport.addTransportListener((sourceTransport, incomingAddress, wholeMessage, tmStateReference) -> {
            System.out.println();
        });
        snmp = new Snmp(transport);

        transport.listen();
    }

    public String sendRequest(int pduType) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(pduType);

        ResponseEvent responseEvent = snmp.send(pdu, target);
        return responseEvent.getResponse().get(0).getVariable().toString();
    }
}
