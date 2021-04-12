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

import org.snmp4j.UserTarget;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import java.io.IOException;

public class SnmpDeviceSimulatorV3 extends SnmpDeviceSimulatorV2 {
    public SnmpDeviceSimulatorV3(int port, String securityName, String authenticationPassphrase, String privacyPassphrase) throws IOException {
        super(12, null);
//        super(new File("conf.agent"), new File("bootCounter.agent"));

        USM usm = new USM();
        SecurityModels.getInstance().addSecurityModel(usm);

        OID authenticationProtocol = AuthSHA.ID;
        OID privacyProtocol = PrivDES.ID;

        UsmUser user = new UsmUser(new OctetString(securityName), authenticationProtocol, new OctetString(authenticationPassphrase), privacyProtocol, new OctetString(privacyPassphrase));
    }

    public void initV3(UsmUser user, String securityName) {
//        snmp.getUSM().addUser(user);

        UserTarget userTarget = new UserTarget();
        userTarget.setSecurityName(new OctetString(securityName));
        userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
    }

}
