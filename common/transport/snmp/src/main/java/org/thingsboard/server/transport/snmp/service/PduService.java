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
package org.thingsboard.server.transport.snmp.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.util.TypeCastUtil;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class PduService {

    @Value("${transport.snmp.max_request_oids:100}")
    private int maxRequestOids;

    @Value("${transport.snmp.response.ignore_type_cast_errors:false}")
    private boolean ignoreTypeCastErrors;

    public List<PDU> createPdus(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        List<PDU> pdus = new ArrayList<>();
        List<SnmpMapping> allMappings = communicationConfig.getAllMappings();

        for (List<SnmpMapping> mappings : Lists.partition(allMappings, maxRequestOids)) {
            PDU pdu = setUpPdu(sessionContext);
            pdu.setType(communicationConfig.getMethod().getCode());
            pdu.addAll(mappings.stream()
                    .filter(mapping -> values.isEmpty() || values.containsKey(mapping.getKey()))
                    .map(mapping -> Optional.ofNullable(values.get(mapping.getKey()))
                            .map(value -> {
                                Variable variable = toSnmpVariable(value, mapping.getDataType());
                                return new VariableBinding(new OID(mapping.getOid()), variable);
                            })
                            .orElseGet(() -> new VariableBinding(new OID(mapping.getOid()))))
                    .collect(Collectors.toList()));
            if (pdu.size() > 0) {
                pdus.add(pdu);
            }
        }

        return pdus;
    }

    public PDU createSingleVariablePdu(DeviceSessionContext sessionContext, SnmpMethod snmpMethod, String oid, String value, DataType dataType) {
        PDU pdu = setUpPdu(sessionContext);
        pdu.setType(snmpMethod.getCode());

        Variable variable = value == null ? Null.instance : toSnmpVariable(value, dataType);
        pdu.add(new VariableBinding(new OID(oid), variable));

        return pdu;
    }

    private Variable toSnmpVariable(String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

    private PDU setUpPdu(DeviceSessionContext sessionContext) {
        PDU pdu;
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        SnmpProtocolVersion snmpVersion = deviceTransportConfiguration.getProtocolVersion();
        switch (snmpVersion) {
            case V1:
            case V2C:
                pdu = new PDU();
                break;
            case V3:
                ScopedPDU scopedPdu = new ScopedPDU();
                scopedPdu.setContextName(new OctetString(deviceTransportConfiguration.getContextName()));
                scopedPdu.setContextEngineID(new OctetString(deviceTransportConfiguration.getEngineId()));
                pdu = scopedPdu;
                break;
            default:
                throw new UnsupportedOperationException("SNMP version " + snmpVersion + " is not supported");
        }
        return pdu;
    }


    public JsonObject processPdus(List<PDU> pdus, List<SnmpMapping> responseMappings) {
        Map<OID, String> values = processPdus(pdus);

        Map<OID, SnmpMapping> mappings = new HashMap<>();
        if (responseMappings != null) {
            for (SnmpMapping mapping : responseMappings) {
                OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
            }
        }

        JsonObject data = new JsonObject();
        values.forEach((oid, value) -> {
            log.trace("Processing variable binding: {} - {}", oid, value);

            SnmpMapping mapping = mappings.get(oid);
            if (mapping == null) {
                log.debug("No SNMP mapping for oid {}", oid);
                return;
            }

            processValue(mapping.getKey(), mapping.getDataType(), value, data);
        });

        return data;
    }

    public Map<OID, String> processPdus(List<PDU> pdus) {
        return pdus.stream()
                .flatMap(pdu -> pdu.getVariableBindings().stream())
                .filter(Objects::nonNull)
                .filter(variableBinding -> !(variableBinding.getVariable() instanceof Null))
                .collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::toValueString));
    }

    public void processValue(String key, DataType dataType, String value, JsonObject result) {
        try {
            switch (dataType) {
                case STRING:
                case JSON:
                    result.addProperty(key, value);
                    break;
                case LONG:
                case DOUBLE:
                    result.addProperty(key, TypeCastUtil.castToNumber(value).getValue());
                    break;
                case BOOLEAN:
                    if (StringUtils.equalsAnyIgnoreCase(value, "true", "false")) {
                        result.addProperty(key, Boolean.parseBoolean(value));
                    } else {
                        throw new IllegalArgumentException("Can't parse '" + value + "' as boolean");
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            if (ignoreTypeCastErrors) {
                log.debug("Ignoring value '{}' for key '{}' because of data type mismatch ({} required)", value, key, dataType);
            } else {
                throw e;
            }
        }
    }

}
