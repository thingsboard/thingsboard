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

import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOMutableTableModel;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.agent.mo.ext.AgentppSimulationMib;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.TransportDomains;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.mo.snmp4j.example.Snmp4jHeartbeatMib;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * The TestAgent is a sample SNMP agent implementation of all
 * features (MIB implementations) provided by the SNMP4J-Agent framework.
 *
 * Note, for snmp4s, this code is mostly a copy from snmp4j.
 * And don't remove snmp users
 *
 */
public class SnmpDeviceSimulatorV3 extends BaseAgent {
    protected String address;
    private Snmp4jHeartbeatMib heartbeatMIB;
    private AgentppSimulationMib agentppSimulationMIB;

    public SnmpDeviceSimulatorV3(CommandProcessor processor) throws IOException {
        super(new File("SNMP4JTestAgentBC.cfg"), new File("SNMP4JTestAgentConfig.cfg"),
                processor);
        agent.setWorkerPool(ThreadPool.create("RequestPool", 4));
    }

    public void setUpMappings(Map<String, String> oidToResponseMappings) {
        unregisterManagedObject(getSnmpv2MIB());
        oidToResponseMappings.forEach((oid, response) -> {
            registerManagedObject(new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_WRITE, new OctetString(response)));
        });
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

    protected void registerManagedObjects() {
        try {
            server.register(createStaticIfTable(), null);
            server.register(createStaticIfXTable(), null);
            agentppSimulationMIB.registerMOs(server, null);
            heartbeatMIB.registerMOs(server, null);
        } catch (DuplicateRegistrationException ex) {
            ex.printStackTrace();
        }
    }

    protected void addNotificationTargets(SnmpTargetMIB targetMIB,
                                          SnmpNotificationMIB notificationMIB) {
        targetMIB.addDefaultTDomains();

        targetMIB.addTargetAddress(new OctetString("notificationV2c"),
                TransportDomains.transportDomainUdpIpv4,
                new OctetString(new UdpAddress("127.0.0.1/162").getValue()),
                200, 1,
                new OctetString("notify"),
                new OctetString("v2c"),
                StorageType.permanent);
        targetMIB.addTargetAddress(new OctetString("notificationV3"),
                TransportDomains.transportDomainUdpIpv4,
                new OctetString(new UdpAddress("127.0.0.1/1162").getValue()),
                200, 1,
                new OctetString("notify"),
                new OctetString("v3notify"),
                StorageType.permanent);
        targetMIB.addTargetParams(new OctetString("v2c"),
                MessageProcessingModel.MPv2c,
                SecurityModel.SECURITY_MODEL_SNMPv2c,
                new OctetString("cpublic"),
                SecurityLevel.AUTH_PRIV,
                StorageType.permanent);
        targetMIB.addTargetParams(new OctetString("v3notify"),
                MessageProcessingModel.MPv3,
                SecurityModel.SECURITY_MODEL_USM,
                new OctetString("v3notify"),
                SecurityLevel.NOAUTH_NOPRIV,
                StorageType.permanent);
        notificationMIB.addNotifyEntry(new OctetString("default"),
                new OctetString("notify"),
                SnmpNotificationMIB.SnmpNotifyTypeEnum.inform,
                StorageType.permanent);
    }
    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1,
                new OctetString("cpublic"),
                new OctetString("v1v2group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
                new OctetString("cpublic"),
                new OctetString("v1v2group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHADES"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("MD5DES"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("TEST"),
                new OctetString("v3test"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHA"),
                new OctetString("v3restricted"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHAAES128"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHAAES192"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHAAES256"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("MD5AES128"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("MD5AES192"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("MD5AES256"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("aboba"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        //============================================//
        // agent5-auth-priv
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("agent5"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        //===========================================//
        // agent002
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("agent002"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        //===========================================//
        // user001-auth-no-priv
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("user001"),
                new OctetString("group001"),
                StorageType.nonVolatile);
        //===========================================//

        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("v3notify"),
                new OctetString("v3group"),
                StorageType.nonVolatile);

        //===========================================//
        // group auth no priv
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("v3notify-auth"),
                new OctetString("group001"),
                StorageType.nonVolatile);
        //===========================================//



        // my conf
        vacm.addAccess(new OctetString("group001"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.AUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY,
                SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);
        vacm.addAccess(new OctetString("v3group"), new OctetString(),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.AUTH_PRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);
        vacm.addAccess(new OctetString("v3restricted"), new OctetString(),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("restrictedReadView"),
                new OctetString("restrictedWriteView"),
                new OctetString("restrictedNotifyView"),
                StorageType.nonVolatile);
        vacm.addAccess(new OctetString("v3test"), new OctetString(),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.AUTH_PRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("testReadView"),
                new OctetString("testWriteView"),
                new OctetString("testNotifyView"),
                StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("restrictedReadView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedWriteView"),
                new OID("1.3.6.1.2.1"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                new OID("1.3.6.1.6.3.1"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("testReadView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testReadView"),
                new OID("1.3.6.1.2.1.1"),
                new OctetString(), VacmMIB.vacmViewExcluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testWriteView"),
                new OID("1.3.6.1.2.1"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testNotifyView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

    }

    protected void addUsmUser(USM usm) {
        UsmUser user = new UsmUser(new OctetString("SHADES"),
                AuthSHA.ID,
                new OctetString("SHADESAuthPassword"),
                PrivDES.ID,
                new OctetString("SHADESPrivPassword"));
//    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        usm.addUser(user.getSecurityName(), null, user);
        user = new UsmUser(new OctetString("TEST"),
                AuthSHA.ID,
                new OctetString("maplesyrup"),
                PrivDES.ID,
                new OctetString("maplesyrup"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHA"),
                AuthSHA.ID,
                new OctetString("SHAAuthPassword"),
                null,
                null);
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHADES"),
                AuthSHA.ID,
                new OctetString("SHADESAuthPassword"),
                PrivDES.ID,
                new OctetString("SHADESPrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("MD5DES"),
                AuthMD5.ID,
                new OctetString("MD5DESAuthPassword"),
                PrivDES.ID,
                new OctetString("MD5DESPrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHAAES128"),
                AuthSHA.ID,
                new OctetString("SHAAES128AuthPassword"),
                PrivAES128.ID,
                new OctetString("SHAAES128PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHAAES192"),
                AuthSHA.ID,
                new OctetString("SHAAES192AuthPassword"),
                PrivAES192.ID,
                new OctetString("SHAAES192PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHAAES256"),
                AuthSHA.ID,
                new OctetString("SHAAES256AuthPassword"),
                PrivAES256.ID,
                new OctetString("SHAAES256PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);

        user = new UsmUser(new OctetString("MD5AES128"),
                AuthMD5.ID,
                new OctetString("MD5AES128AuthPassword"),
                PrivAES128.ID,
                new OctetString("MD5AES128PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("MD5AES192"),
                AuthHMAC192SHA256.ID,
                new OctetString("MD5AES192AuthPassword"),
                PrivAES192.ID,
                new OctetString("MD5AES192PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        //==============================================================
        user = new UsmUser(new OctetString("MD5AES256"),
                AuthMD5.ID,
                new OctetString("MD5AES256AuthPassword"),
                PrivAES256.ID,
                new OctetString("MD5AES256PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("MD5AES256"),
                AuthMD5.ID,
                new OctetString("MD5AES256AuthPassword"),
                PrivAES256.ID,
                new OctetString("MD5AES256PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);

        OctetString securityName = new OctetString("aboba");
        OctetString authenticationPassphrase = new OctetString("abobaaboba");
        OctetString privacyPassphrase = new OctetString("abobaaboba");
        OID authenticationProtocol = AuthSHA.ID;
        OID privacyProtocol = PrivDES.ID; // FIXME: to config
        user = new UsmUser(securityName, authenticationProtocol, authenticationPassphrase, privacyProtocol, privacyPassphrase);
        usm.addUser(user);

        //===============================================================//
        user = new UsmUser(new OctetString("agent5"),
                AuthSHA.ID,
                new OctetString("authpass"),
                PrivDES.ID,
                new OctetString("privpass"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        //===============================================================//
        // user001
        user = new UsmUser(new OctetString("user001"),
                AuthSHA.ID,
                new OctetString("authpass"),
                null, null);
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        //===============================================================//
        // user002
        user = new UsmUser(new OctetString("user001"),
                null,
                null,
                null, null);
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        //===============================================================//

        user = new UsmUser(new OctetString("v3notify"),
                null,
                null,
                null,
                null);
        usm.addUser(user.getSecurityName(), null, user);

        this.usm = usm;
    }

    private static DefaultMOTable createStaticIfXTable() {
        MOTableSubIndex[] subIndexes =
                new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
        MOTableIndex indexDef = new MOTableIndex(subIndexes, false);
        MOColumn[] columns = new MOColumn[19];
        int c = 0;
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_OCTET_STRING,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifName
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifInMulticastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifInBroadcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifOutMulticastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifOutBroadcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCInOctets
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCInUcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCInMulticastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCInBroadcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCOutOctets
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCOutUcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCOutMulticastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_COUNTER32,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifHCOutBroadcastPkts
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_WRITE);     // ifLinkUpDownTrapEnable
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_GAUGE32,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifHighSpeed
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_WRITE);     // ifPromiscuousMode
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifConnectorPresent
        columns[c++] =
                new MOMutableColumn(c, SMIConstants.SYNTAX_OCTET_STRING,     // ifAlias
                        MOAccessImpl.ACCESS_READ_WRITE, null);
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_TIMETICKS,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifCounterDiscontinuityTime

        DefaultMOTable ifXTable =
                new DefaultMOTable(new OID("1.3.6.1.2.1.31.1.1.1"), indexDef, columns);
        MOMutableTableModel model = (MOMutableTableModel) ifXTable.getModel();
        Variable[] rowValues1 = new Variable[] {
                new OctetString("Ethernet-0"),
                new Integer32(1),
                new Integer32(2),
                new Integer32(3),
                new Integer32(4),
                new Integer32(5),
                new Integer32(6),
                new Integer32(7),
                new Integer32(8),
                new Integer32(9),
                new Integer32(10),
                new Integer32(11),
                new Integer32(12),
                new Integer32(13),
                new Integer32(14),
                new Integer32(15),
                new Integer32(16),
                new OctetString("My eth"),
                new TimeTicks(1000)
        };
        Variable[] rowValues2 = new Variable[] {
                new OctetString("Loopback"),
                new Integer32(21),
                new Integer32(22),
                new Integer32(23),
                new Integer32(24),
                new Integer32(25),
                new Integer32(26),
                new Integer32(27),
                new Integer32(28),
                new Integer32(29),
                new Integer32(30),
                new Integer32(31),
                new Integer32(32),
                new Integer32(33),
                new Integer32(34),
                new Integer32(35),
                new Integer32(36),
                new OctetString("My loop"),
                new TimeTicks(2000)
        };
        model.addRow(new DefaultMOMutableRow2PC(new OID("1"), rowValues1));
        model.addRow(new DefaultMOMutableRow2PC(new OID("2"), rowValues2));
        ifXTable.setVolatile(true);
        return ifXTable;
    }

    private static DefaultMOTable createStaticIfTable() {
        MOTableSubIndex[] subIndexes =
                new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
        MOTableIndex indexDef = new MOTableIndex(subIndexes, false);
        MOColumn[] columns = new MOColumn[8];
        int c = 0;
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifIndex
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_OCTET_STRING,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifDescr
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifType
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifMtu
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_GAUGE32,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifSpeed
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_OCTET_STRING,
                        MOAccessImpl.ACCESS_READ_ONLY); // ifPhysAddress
        columns[c++] =
                new MOMutableColumn(c, SMIConstants.SYNTAX_INTEGER,     // ifAdminStatus
                        MOAccessImpl.ACCESS_READ_WRITE, null);
        columns[c++] =
                new MOColumn(c, SMIConstants.SYNTAX_INTEGER,
                        MOAccessImpl.ACCESS_READ_ONLY);     // ifOperStatus

        DefaultMOTable ifTable =
                new DefaultMOTable(new OID("1.3.6.1.2.1.2.2.1"), indexDef, columns);
        MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
        Variable[] rowValues1 = new Variable[] {
                new Integer32(1),
                new OctetString("eth0"),
                new Integer32(6),
                new Integer32(1500),
                new Gauge32(100000000),
                new OctetString("00:00:00:00:01"),
                new Integer32(1),
                new Integer32(1)
        };
        Variable[] rowValues2 = new Variable[] {
                new Integer32(2),
                new OctetString("loopback"),
                new Integer32(24),
                new Integer32(1500),
                new Gauge32(10000000),
                new OctetString("00:00:00:00:02"),
                new Integer32(1),
                new Integer32(1)
        };
        model.addRow(new DefaultMOMutableRow2PC(new OID("1"), rowValues1));
        model.addRow(new DefaultMOMutableRow2PC(new OID("2"), rowValues2));
        ifTable.setVolatile(true);
        return ifTable;
    }

    private static DefaultMOTable createStaticSnmp4sTable() {
        MOTableSubIndex[] subIndexes =
                new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
        MOTableIndex indexDef = new MOTableIndex(subIndexes, false);
        MOColumn[] columns = new MOColumn[8];
        int c = 0;
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_NULL, MOAccessImpl.ACCESS_READ_ONLY); // testNull
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY); // testBoolean
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY); // ifType
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY); // ifMtu
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY); // ifSpeed
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY); //ifPhysAddress
        columns[c++] = new MOMutableColumn(c, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_WRITE,
                null);
        // ifAdminStatus
        columns[c++] = new MOColumn(c, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY);
        // ifOperStatus

        DefaultMOTable ifTable =
                new DefaultMOTable(new OID("1.3.6.1.4.1.50000.1.1"), indexDef, columns);
        MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
        Variable[] rowValues1 = new Variable[] {
                new Integer32(1),
                new OctetString("eth0"),
                new Integer32(6),
                new Integer32(1500),
                new Gauge32(100000000),
                new OctetString("00:00:00:00:01"),
                new Integer32(1),
                new Integer32(1)
        };
        Variable[] rowValues2 = new Variable[] {
                new Integer32(2),
                new OctetString("loopback"),
                new Integer32(24),
                new Integer32(1500),
                new Gauge32(10000000),
                new OctetString("00:00:00:00:02"),
                new Integer32(1),
                new Integer32(1)
        };
        model.addRow(new DefaultMOMutableRow2PC(new OID("1"), rowValues1));
        model.addRow(new DefaultMOMutableRow2PC(new OID("2"), rowValues2));
        ifTable.setVolatile(true);
        return ifTable;
    }

    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping[2];
        Address addr = GenericAddress.parse(address);
        TransportMapping tm =
                TransportMappings.getInstance().createTransportMapping(addr);
        transportMappings[0] = tm;
        transportMappings[1] = new DefaultTcpTransportMapping(new TcpAddress(address));
    }

    public void start(String ip, String port) throws IOException {
        address = ip + "/" + port;
        //BasicConfigurator.configure();
        init();
        addShutdownHook();
//        loadConfig(ImportModes.REPLACE_CREATE);
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
    }

    protected void unregisterManagedObjects() {
        // here we should unregister those objects previously registered...
    }

    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[] {
                new OctetString("public"),              // community name
                new OctetString("cpublic"),              // security name
                getAgent().getContextEngineID(),        // local engine ID
                new OctetString("public"),              // default context name
                new OctetString(),                      // transport tag
                new Integer32(StorageType.nonVolatile), // storage type
                new Integer32(RowStatus.active)         // row status
        };
        MOTableRow row =
                communityMIB.getSnmpCommunityEntry().createRow(
                        new OctetString("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow((SnmpCommunityMIB.SnmpCommunityEntryRow) row);
//    snmpCommunityMIB.setSourceAddressFiltering(true);
    }

    protected void registerSnmpMIBs() {
        heartbeatMIB = new Snmp4jHeartbeatMib(super.getNotificationOriginator(),
                new OctetString(),
                super.snmpv2MIB.getSysUpTime());
        agentppSimulationMIB = new AgentppSimulationMib();
        super.registerSnmpMIBs();
    }

    protected void initMessageDispatcher() {
        this.dispatcher = new MessageDispatcherImpl();
        this.mpv3 = new MPv3(this.agent.getContextEngineID().getValue());
        this.usm = new USM(SecurityProtocols.getInstance(), this.agent.getContextEngineID(), this.updateEngineBoots());
        SecurityModels.getInstance().addSecurityModel(this.usm);
        SecurityProtocols.getInstance().addDefaultProtocols();
        this.dispatcher.addMessageProcessingModel(new MPv1());
        this.dispatcher.addMessageProcessingModel(new MPv2c());
        this.dispatcher.addMessageProcessingModel(this.mpv3);
        this.initSnmpSession();
    }

}