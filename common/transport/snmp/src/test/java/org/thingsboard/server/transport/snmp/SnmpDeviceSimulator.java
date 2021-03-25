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
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class SnmpDeviceSimulator extends BaseAgent {

    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulator device = new SnmpDeviceSimulator(1610);

        device.start();
        device.setUpMappings(Map.of(
                ".1.3.6.1.2.1.1.1.50", "12",
                ".1.3.6.1.2.1.2.1.52", "56",
                ".1.3.6.1.2.1.3.1.54", "yes"
        ));

        new Scanner(System.in).nextLine();
    }


    private final Target target;
    private final Address address;

    public SnmpDeviceSimulator(int port) {
        super(new File("conf.agent"), new File("bootCounter.agent"),
                new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        address = GenericAddress.parse("udp:0.0.0.0/" + port);
        target.setAddress(address);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        this.target = target;
    }

    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
    }

    public void setUpMappings(Map<String, String> oidToResponseMappings) {
        unregisterManagedObject(getSnmpv2MIB());
        oidToResponseMappings.forEach((oid, response) -> {
            registerManagedObject(new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(response)));
        });
    }

    public Target getTarget() {
        return target;
    }

    @Override
    protected void registerManagedObjects() {
    }

    protected void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void unregisterManagedObject(MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB,
                                          SnmpNotificationMIB notificationMIB) {
    }

    @Override
    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
                        "cpublic"), new OctetString("v1v2group"),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
                new OctetString("fullWriteView"), new OctetString(
                        "fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }

    protected void addUsmUser(USM usm) {
    }

    protected void initTransportMappings() {
        transportMappings = new TransportMapping[]{TransportMappings.getInstance().createTransportMapping(address)};
    }

    protected void unregisterManagedObjects() {
    }

    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
                new OctetString("public"),
                new OctetString("cpublic"),
                getAgent().getContextEngineID(),
                new OctetString("public"),
                new OctetString(),
                new Integer32(StorageType.nonVolatile),
                new Integer32(RowStatus.active)
        };
        SnmpCommunityMIB.SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }
}
