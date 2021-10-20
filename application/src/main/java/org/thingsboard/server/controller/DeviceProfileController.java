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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class DeviceProfileController extends BaseController {

    private static final String COAP_TRANSPORT_CONFIGURATION_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"type\":\"COAP\",\n" +
            "   \"clientSettings\":{\n" +
            "      \"edrxCycle\":null,\n" +
            "      \"powerMode\":\"DRX\",\n" +
            "      \"psmActivityTimer\":null,\n" +
            "      \"pagingTransmissionWindow\":null\n" +
            "   },\n" +
            "   \"coapDeviceTypeConfiguration\":{\n" +
            "      \"coapDeviceType\":\"DEFAULT\",\n" +
            "      \"transportPayloadTypeConfiguration\":{\n" +
            "         \"transportPayloadType\":\"JSON\"\n" +
            "      }\n" +
            "   }\n" +
            "}"
            + MARKDOWN_CODE_BLOCK_END;

    private static final String TRANSPORT_CONFIGURATION = "# Transport Configuration" + NEW_LINE +
            "5 transport configuration types are available:\n" +
            " * 'DEFAULT';\n" +
            " * 'MQTT';\n" +
            " * 'LWM2M';\n" +
            " * 'COAP';\n" +
            " * 'SNMP'." + NEW_LINE + "Default type supports basic MQTT, HTTP, CoAP and LwM2M transports. " +
            "Please refer to the [docs](https://thingsboard.io/docs/user-guide/device-profiles/#transport-configuration) for more details about other types.\n" +
            "\nSee another example of COAP transport configuration below:" + NEW_LINE + COAP_TRANSPORT_CONFIGURATION_EXAMPLE;

    private static final String ALARM_FILTER_KEY = "## Alarm Filter Key" + NEW_LINE +
            "Filter Key defines either entity field, attribute, telemetry or constant. It is a JSON object that consists the key name and type. The following filter key types are supported:\n" +
            " * 'ATTRIBUTE' - used for attributes values;\n" +
            " * 'TIME_SERIES' - used for time-series values;\n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type;\n" +
            " * 'CONSTANT' - constant value specified." + NEW_LINE + "Let's review the example:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;

    private static final String FILTER_PREDICATE = NEW_LINE + "## Filter Predicate" + NEW_LINE +
            "Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. " +
            "Platform supports 4 predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows to combine multiple operations over one filter key." + NEW_LINE +
            "Simple predicate example to check 'value < 100': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"LESS\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 100,\n" +
            "    \"dynamicValue\": null\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Complex predicate example, to check 'value < 10 or value > 20': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"GREATER\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 20,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "More complex predicate example, to check 'value < 10 or (value > 50 && value < 60)': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"COMPLEX\",\n" +
            "      \"operation\": \"AND\",\n" +
            "      \"predicates\": [\n" +
            "        {\n" +
            "          \"operation\": \"GREATER\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 50,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"operation\": \"LESS\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 60,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic " +
            "expression (for example, temperature > value of the tenant attribute with key 'temperatureThreshold'). " +
            "It is possible to use 'dynamicValue' to define attribute of the tenant, customer or device. " +
            "See example below:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"GREATER\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 0,\n" +
            "    \"dynamicValue\": {\n" +
            "      \"inherit\": false,\n" +
            "      \"sourceType\": \"CURRENT_TENANT\",\n" +
            "      \"sourceAttribute\": \"temperatureThreshold\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Note that you may use 'CURRENT_DEVICE', 'CURRENT_CUSTOMER' and 'CURRENT_TENANT' as a 'sourceType'. The 'defaultValue' is used when the attribute with such a name is not defined for the chosen source. " +
            "The 'sourceAttribute' can be inherited from the owner of the specified 'sourceType' if 'inherit' is set to true.";

    private static final String KEY_FILTERS_DESCRIPTION = "# Key Filters" + NEW_LINE +
            "Key filter objects are created under the **'condition'** array. They allow you to define complex logical expressions over entity field, " +
            "attribute, latest time-series value or constant. The filter is defined using 'key', 'valueType', " +
            "'value' (refers to the value of the 'CONSTANT' alarm filter key type) and 'predicate' objects. Let's review each object:" + NEW_LINE +
            ALARM_FILTER_KEY + FILTER_VALUE_TYPE + NEW_LINE + FILTER_PREDICATE + NEW_LINE;

    private static final String DEFAULT_DEVICE_PROFILE_DATA_EXAMPLE = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"alarms\":[\n" +
            "   ],\n" +
            "   \"configuration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   },\n" +
            "   \"provisionConfiguration\":{\n" +
            "      \"type\":\"DISABLED\",\n" +
            "      \"provisionDeviceSecret\":null\n" +
            "   },\n" +
            "   \"transportConfiguration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   }\n" +
            "}" + MARKDOWN_CODE_BLOCK_END;

    private static final String CUSTOM_DEVICE_PROFILE_DATA_EXAMPLE = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"alarms\":[\n" +
            "      {\n" +
            "         \"id\":\"2492b935-1226-59e9-8615-17d8978a4f93\",\n" +
            "         \"alarmType\":\"Temperature Alarm\",\n" +
            "         \"clearRule\":{\n" +
            "            \"schedule\":null,\n" +
            "            \"condition\":{\n" +
            "               \"spec\":{\n" +
            "                  \"type\":\"SIMPLE\"\n" +
            "               },\n" +
            "               \"condition\":[\n" +
            "                  {\n" +
            "                     \"key\":{\n" +
            "                        \"key\":\"temperature\",\n" +
            "                        \"type\":\"TIME_SERIES\"\n" +
            "                     },\n" +
            "                     \"value\":null,\n" +
            "                     \"predicate\":{\n" +
            "                        \"type\":\"NUMERIC\",\n" +
            "                        \"value\":{\n" +
            "                           \"userValue\":null,\n" +
            "                           \"defaultValue\":30.0,\n" +
            "                           \"dynamicValue\":null\n" +
            "                        },\n" +
            "                        \"operation\":\"LESS\"\n" +
            "                     },\n" +
            "                     \"valueType\":\"NUMERIC\"\n" +
            "                  }\n" +
            "               ]\n" +
            "            },\n" +
            "            \"dashboardId\":null,\n" +
            "            \"alarmDetails\":null\n" +
            "         },\n" +
            "         \"propagate\":false,\n" +
            "         \"createRules\":{\n" +
            "            \"MAJOR\":{\n" +
            "               \"schedule\":{\n" +
            "                  \"type\":\"SPECIFIC_TIME\",\n" +
            "                  \"endsOn\":64800000,\n" +
            "                  \"startsOn\":43200000,\n" +
            "                  \"timezone\":\"Europe/Kiev\",\n" +
            "                  \"daysOfWeek\":[\n" +
            "                     1,\n" +
            "                     3,\n" +
            "                     5\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"DURATION\",\n" +
            "                     \"unit\":\"MINUTES\",\n" +
            "                     \"predicate\":{\n" +
            "                        \"userValue\":null,\n" +
            "                        \"defaultValue\":30,\n" +
            "                        \"dynamicValue\":null\n" +
            "                     }\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"temperature\",\n" +
            "                           \"type\":\"TIME_SERIES\"\n" +
            "                        },\n" +
            "                        \"value\":null,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"COMPLEX\",\n" +
            "                           \"operation\":\"OR\",\n" +
            "                           \"predicates\":[\n" +
            "                              {\n" +
            "                                 \"type\":\"NUMERIC\",\n" +
            "                                 \"value\":{\n" +
            "                                    \"userValue\":null,\n" +
            "                                    \"defaultValue\":50.0,\n" +
            "                                    \"dynamicValue\":null\n" +
            "                                 },\n" +
            "                                 \"operation\":\"LESS_OR_EQUAL\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"type\":\"NUMERIC\",\n" +
            "                                 \"value\":{\n" +
            "                                    \"userValue\":null,\n" +
            "                                    \"defaultValue\":30.0,\n" +
            "                                    \"dynamicValue\":null\n" +
            "                                 },\n" +
            "                                 \"operation\":\"GREATER\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            },\n" +
            "            \"WARNING\":{\n" +
            "               \"schedule\":{\n" +
            "                  \"type\":\"CUSTOM\",\n" +
            "                  \"items\":[\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":1\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":64800000,\n" +
            "                        \"enabled\":true,\n" +
            "                        \"startsOn\":43200000,\n" +
            "                        \"dayOfWeek\":2\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":3\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":57600000,\n" +
            "                        \"enabled\":true,\n" +
            "                        \"startsOn\":36000000,\n" +
            "                        \"dayOfWeek\":4\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":5\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":6\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":7\n" +
            "                     }\n" +
            "                  ],\n" +
            "                  \"timezone\":\"Europe/Kiev\"\n" +
            "               },\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"REPEATING\",\n" +
            "                     \"predicate\":{\n" +
            "                        \"userValue\":null,\n" +
            "                        \"defaultValue\":5,\n" +
            "                        \"dynamicValue\":null\n" +
            "                     }\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"tempConstant\",\n" +
            "                           \"type\":\"CONSTANT\"\n" +
            "                        },\n" +
            "                        \"value\":30,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"NUMERIC\",\n" +
            "                           \"value\":{\n" +
            "                              \"userValue\":null,\n" +
            "                              \"defaultValue\":0.0,\n" +
            "                              \"dynamicValue\":{\n" +
            "                                 \"inherit\":false,\n" +
            "                                 \"sourceType\":\"CURRENT_DEVICE\",\n" +
            "                                 \"sourceAttribute\":\"tempThreshold\"\n" +
            "                              }\n" +
            "                           },\n" +
            "                           \"operation\":\"EQUAL\"\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            },\n" +
            "            \"CRITICAL\":{\n" +
            "               \"schedule\":null,\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"SIMPLE\"\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"temperature\",\n" +
            "                           \"type\":\"TIME_SERIES\"\n" +
            "                        },\n" +
            "                        \"value\":null,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"NUMERIC\",\n" +
            "                           \"value\":{\n" +
            "                              \"userValue\":null,\n" +
            "                              \"defaultValue\":50.0,\n" +
            "                              \"dynamicValue\":null\n" +
            "                           },\n" +
            "                           \"operation\":\"GREATER\"\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            }\n" +
            "         },\n" +
            "         \"propagateRelationTypes\":null\n" +
            "      }\n" +
            "   ],\n" +
            "   \"configuration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   },\n" +
            "   \"provisionConfiguration\":{\n" +
            "      \"type\":\"ALLOW_CREATE_NEW_DEVICES\",\n" +
            "      \"provisionDeviceSecret\":\"vaxb9hzqdbz3oqukvomg\"\n" +
            "   },\n" +
            "   \"transportConfiguration\":{\n" +
            "      \"type\":\"MQTT\",\n" +
            "      \"deviceTelemetryTopic\":\"v1/devices/me/telemetry\",\n" +
            "      \"deviceAttributesTopic\":\"v1/devices/me/attributes\",\n" +
            "      \"transportPayloadTypeConfiguration\":{\n" +
            "         \"transportPayloadType\":\"PROTOBUF\",\n" +
            "         \"deviceTelemetryProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage telemetry;\\n\\nmessage SensorDataReading {\\n\\n  optional double temperature = 1;\\n  optional double humidity = 2;\\n  InnerObject innerObject = 3;\\n\\n  message InnerObject {\\n    optional string key1 = 1;\\n    optional bool key2 = 2;\\n    optional double key3 = 3;\\n    optional int32 key4 = 4;\\n    optional string key5 = 5;\\n  }\\n}\",\n" +
            "         \"deviceAttributesProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage attributes;\\n\\nmessage SensorConfiguration {\\n  optional string firmwareVersion = 1;\\n  optional string serialNumber = 2;\\n}\",\n" +
            "         \"deviceRpcRequestProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage rpc;\\n\\nmessage RpcRequestMsg {\\n  optional string method = 1;\\n  optional int32 requestId = 2;\\n  optional string params = 3;\\n}\",\n" +
            "         \"deviceRpcResponseProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage rpc;\\n\\nmessage RpcResponseMsg {\\n  optional string payload = 1;\\n}\"\n" +
            "      }\n" +
            "   }\n" +
            "}" + MARKDOWN_CODE_BLOCK_END;
    private static final String DEVICE_PROFILE_DATA_DEFINITION = NEW_LINE + "# Device profile data definition" + NEW_LINE +
            "Device profile data object contains alarm rules configuration, device provision strategy and transport type configuration for device connectivity. Let's review some examples. " +
            "First one is the default device profile data configuration and second one - the custom one. " +
            NEW_LINE + DEFAULT_DEVICE_PROFILE_DATA_EXAMPLE + NEW_LINE + CUSTOM_DEVICE_PROFILE_DATA_EXAMPLE +
            NEW_LINE + "Let's review some specific objects examples related to the device profile configuration:";

    private static final String ALARM_SCHEDULE = NEW_LINE + "# Alarm Schedule" + NEW_LINE +
            "Alarm Schedule JSON object represents the time interval during which the alarm rule is active. Note, " +
            NEW_LINE + DEVICE_PROFILE_ALARM_SCHEDULE_ALWAYS_EXAMPLE + NEW_LINE + "means alarm rule is active all the time. " +
            "**'daysOfWeek'** field represents Monday as 1, Tuesday as 2 and so on. **'startsOn'** and **'endsOn'** fields represent hours in millis (e.g. 64800000 = 18:00 or 6pm). " +
            "**'enabled'** flag specifies if item in a custom rule is active for specific day of the week:" + NEW_LINE +
            "## Specific Time Schedule" + NEW_LINE +
            DEVICE_PROFILE_ALARM_SCHEDULE_SPECIFIC_TIME_EXAMPLE + NEW_LINE +
            "## Custom Schedule" +
            NEW_LINE + DEVICE_PROFILE_ALARM_SCHEDULE_CUSTOM_EXAMPLE + NEW_LINE;

    private static final String ALARM_CONDITION_TYPE = "# Alarm condition type (**'spec'**)" + NEW_LINE +
            "Alarm condition type can be either simple, duration, or repeating. For example, 5 times in a row or during 5 minutes." + NEW_LINE +
            "Note, **'userValue'** field is not used and reserved for future usage, **'dynamicValue'** is used for condition appliance by using the value of the **'sourceAttribute'** " +
            "or else **'defaultValue'** is used (if **'sourceAttribute'** is absent).\n" +
            "\n**'sourceType'** of the **'sourceAttribute'** can be: \n" +
            " * 'CURRENT_DEVICE';\n" +
            " * 'CURRENT_CUSTOMER';\n" +
            " * 'CURRENT_TENANT'." + NEW_LINE +
            "**'sourceAttribute'** can be inherited from the owner if **'inherit'** is set to true (for CURRENT_DEVICE and CURRENT_CUSTOMER)." + NEW_LINE +
            "## Repeating alarm condition" + NEW_LINE +
            DEVICE_PROFILE_ALARM_CONDITION_REPEATING_EXAMPLE + NEW_LINE +
            "## Duration alarm condition" + NEW_LINE +
            DEVICE_PROFILE_ALARM_CONDITION_DURATION_EXAMPLE + NEW_LINE +
            "**'unit'** can be: \n" +
            " * 'SECONDS';\n" +
            " * 'MINUTES';\n" +
            " * 'HOURS';\n" +
            " * 'DAYS'." + NEW_LINE;

    private static final String PROVISION_CONFIGURATION = "# Provision Configuration" + NEW_LINE +
            "There are 3 types of device provision configuration for the device profile: \n" +
            " * 'DISABLED';\n" +
            " * 'ALLOW_CREATE_NEW_DEVICES';\n" +
            " * 'CHECK_PRE_PROVISIONED_DEVICES'." + NEW_LINE +
            "Please refer to the [docs](https://thingsboard.io/docs/user-guide/device-provisioning/) for more details." + NEW_LINE;

    private static final String DEVICE_PROFILE_DATA = DEVICE_PROFILE_DATA_DEFINITION + ALARM_SCHEDULE + ALARM_CONDITION_TYPE +
            KEY_FILTERS_DESCRIPTION + PROVISION_CONFIGURATION + TRANSPORT_CONFIGURATION;

    private static final String DEVICE_PROFILE_ID = "deviceProfileId";

    @Autowired
    private TimeseriesService timeseriesService;

    @ApiOperation(value = "Get Device Profile (getDeviceProfileById)",
            notes = "Fetch the Device Profile object based on the provided Device Profile Id. " +
                    "The server checks that the device profile is owned by the same tenant. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfile getDeviceProfileById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return checkDeviceProfileId(deviceProfileId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profile Info (getDeviceProfileInfoById)",
            notes = "Fetch the Device Profile Info object based on the provided Device Profile Id. "
                    + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDeviceProfileInfoById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return checkNotNull(deviceProfileService.findDeviceProfileInfoById(getTenantId(), deviceProfileId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Default Device Profile (getDefaultDeviceProfileInfo)",
            notes = "Fetch the Default Device Profile Info object. " +
                    DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDefaultDeviceProfileInfo() throws ThingsboardException {
        try {
            return checkNotNull(deviceProfileService.findDefaultDeviceProfileInfo(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get time-series keys (getTimeseriesKeys)",
            notes = "Get a set of unique time-series keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getTimeseriesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        try {
            return timeseriesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get attribute keys (getAttributesKeys)",
            notes = "Get a set of unique attribute keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getAttributesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        try {
            return attributesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Device Profile (saveDevice)",
            notes = "Create or update the Device Profile. When creating device profile, platform generates device profile id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address). " +
                    "The newly created device profile id will be present in the response. " +
                    "Specify existing device profile id to update the device profile. " +
                    "Referencing non-existing device profile Id will cause 'Not Found' error. " + NEW_LINE +
                    "Device profile name is unique in the scope of tenant. Only one 'default' device profile may exist in scope of tenant." + DEVICE_PROFILE_DATA +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile saveDeviceProfile(
            @ApiParam(value = "A JSON value representing the device profile.")
            @RequestBody DeviceProfile deviceProfile) throws ThingsboardException {
        try {
            boolean created = deviceProfile.getId() == null;
            deviceProfile.setTenantId(getTenantId());

            checkEntity(deviceProfile.getId(), deviceProfile, Resource.DEVICE_PROFILE);

            boolean isFirmwareChanged = false;
            boolean isSoftwareChanged = false;

            if (!created) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), deviceProfile.getId());
                if (!Objects.equals(deviceProfile.getFirmwareId(), oldDeviceProfile.getFirmwareId())) {
                    isFirmwareChanged = true;
                }
                if (!Objects.equals(deviceProfile.getSoftwareId(), oldDeviceProfile.getSoftwareId())) {
                    isSoftwareChanged = true;
                }
            }

            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));

            tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(deviceProfile.getTenantId(), savedDeviceProfile.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedDeviceProfile.getId(), savedDeviceProfile,
                    null,
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            otaPackageStateService.update(savedDeviceProfile, isFirmwareChanged, isSoftwareChanged);

            sendEntityNotificationMsg(getTenantId(), savedDeviceProfile.getId(),
                    deviceProfile.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE), deviceProfile,
                    null, deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete device profile (deleteDeviceProfile)",
            notes = "Deletes the device profile. Referencing non-existing device profile Id will cause an error. " +
                    "Can't delete the device profile if it is referenced by existing devices." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.DELETE);
            deviceProfileService.deleteDeviceProfile(getTenantId(), deviceProfileId);

            tbClusterService.onDeviceProfileDelete(deviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(deviceProfile.getTenantId(), deviceProfile.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(deviceProfileId, deviceProfile,
                    null,
                    ActionType.DELETED, null, strDeviceProfileId);

            sendEntityNotificationMsg(getTenantId(), deviceProfile.getId(), EdgeEventActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Make Device Profile Default (setDefaultDeviceProfile)",
            notes = "Marks device profile as default within a tenant scope." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile setDefaultDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.WRITE);
            DeviceProfile previousDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(getTenantId());
            if (deviceProfileService.setDefaultDeviceProfile(getTenantId(), deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), previousDefaultDeviceProfile.getId());

                    logEntityAction(previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            null, ActionType.UPDATED, null);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), deviceProfileId);

                logEntityAction(deviceProfile.getId(), deviceProfile,
                        null, ActionType.UPDATED, null);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.UPDATED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profiles (getDeviceProfiles)",
            notes = "Returns a page of devices profile objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfile> getDeviceProfiles(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfiles(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profiles for transport type (getDeviceProfileInfos)",
            notes = "Returns a page of devices profile info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Type of the transport", allowableValues = TRANSPORT_TYPE_ALLOWABLE_VALUES)
            @RequestParam(required = false) String transportType) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfileInfos(getTenantId(), pageLink, transportType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
