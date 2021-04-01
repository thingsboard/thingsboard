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

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
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
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SnmpDeviceSimulatorV2 extends BaseAgent {

    public static class RequestProcessor extends CommandProcessor {
        private final Consumer<CommandResponderEvent> processor;

        public RequestProcessor(Consumer<CommandResponderEvent> processor) {
            super(new OctetString(MPv3.createLocalEngineID()));
            this.processor = processor;
        }

        @Override
        public void processPdu(CommandResponderEvent event) {
            processor.accept(event);
        }
    }

    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV2 device = new SnmpDeviceSimulatorV2(1610, "public");

        device.start();
        device.setUpMappings(Map.of(
                ".1.3.6.1.2.1.1.1.50", "12",
                ".1.3.6.1.2.1.2.1.52", "56",
                ".1.3.6.1.2.1.3.1.54", "yes",
                ".1.3.6.1.2.1.7.1.58", ""
        ));


//        while (true) {
//            new Scanner(System.in).nextLine();
//            device.sendTrap("127.0.0.1", 1062, Map.of(".1.3.6.1.2.87.1.56", "12"));
//            System.out.println("sent");
//        }

//        Snmp snmp = new Snmp(device.transportMappings[0]);
//        device.snmp.addCommandResponder(event -> {
//            System.out.println(event);
//        });

        new Scanner(System.in).nextLine();
    }


    private final Target target;
    private final Address address;
    private Snmp snmp;

    private final String password;

    public SnmpDeviceSimulatorV2(int port, String password) throws IOException {
        super(new File("conf.agent"), new File("bootCounter.agent"), new RequestProcessor(event -> {
            System.out.println("aboba");
            ((Snmp) event.getSource()).cancel(event.getPDU(), event1 -> System.out.println("canceled"));
        }));
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(password));
        this.address = GenericAddress.parse("udp:0.0.0.0/" + port);
        target.setAddress(address);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        this.target = target;
        this.password = password;
    }

    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
        snmp = new Snmp(transportMappings[0]);
    }

    public void setUpMappings(Map<String, String> oidToResponseMappings) {
        unregisterManagedObject(getSnmpv2MIB());
        oidToResponseMappings.forEach((oid, response) -> {
            registerManagedObject(new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_WRITE, new OctetString(response)));
        });
    }

    public void sendTrap(String host, int port, Map<String, String> values) throws IOException {
        PDU pdu = new PDU();
        pdu.addAll(values.entrySet().stream()
                .map(entry -> new VariableBinding(new OID(entry.getKey()), new OctetString(entry.getValue())))
                .collect(Collectors.toList()));
        pdu.setType(PDU.TRAP);

        CommunityTarget remoteTarget = (CommunityTarget) getTarget().clone();
        remoteTarget.setAddress(new UdpAddress(host + "/" + port));

        snmp.send(pdu, remoteTarget);
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

    public Target getTarget() {
        return target;
    }

}
