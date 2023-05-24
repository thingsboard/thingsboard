package org.thingsboard.server.queue.util;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.FSTUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;


public class DataDecodingEncodingServiceTest {


    private static final int _1M = 1000 * 1000;
    private static String DEVICE_PROFILE_STR = "{\n" +
            "  \"name\": \"thermostat\",\n" +
            "  \"description\": \"Thermostat device profile\",\n" +
            "  \"image\": null,\n" +
            "  \"type\": \"DEFAULT\",\n" +
            "  \"transportType\": \"DEFAULT\",\n" +
            "  \"provisionType\": \"DISABLED\",\n" +
            "  \"defaultRuleChainId\": {\n" +
            "    \"entityType\": \"RULE_CHAIN\",\n" +
            "    \"id\": \"eaaa47b0-e827-11ed-9f13-7f5975adc62f\"\n" +
            "  },\n" +
            "  \"defaultDashboardId\": null,\n" +
            "  \"defaultQueueName\": null,\n" +
            "  \"profileData\": {\n" +
            "    \"configuration\": {\n" +
            "      \"type\": \"DEFAULT\"\n" +
            "    },\n" +
            "    \"transportConfiguration\": {\n" +
            "      \"type\": \"DEFAULT\"\n" +
            "    },\n" +
            "    \"provisionConfiguration\": {\n" +
            "      \"type\": \"DISABLED\",\n" +
            "      \"provisionDeviceSecret\": null\n" +
            "    },\n" +
            "    \"alarms\": [\n" +
            "      {\n" +
            "        \"id\": \"highTemperatureAlarmID\",\n" +
            "        \"alarmType\": \"High Temperature\",\n" +
            "        \"createRules\": {\n" +
            "          \"MAJOR\": {\n" +
            "            \"condition\": {\n" +
            "              \"condition\": [\n" +
            "                {\n" +
            "                  \"key\": {\n" +
            "                    \"type\": \"ATTRIBUTE\",\n" +
            "                    \"key\": \"temperatureAlarmFlag\"\n" +
            "                  },\n" +
            "                  \"valueType\": \"BOOLEAN\",\n" +
            "                  \"value\": null,\n" +
            "                  \"predicate\": {\n" +
            "                    \"type\": \"BOOLEAN\",\n" +
            "                    \"operation\": \"EQUAL\",\n" +
            "                    \"value\": {\n" +
            "                      \"defaultValue\": true,\n" +
            "                      \"userValue\": null,\n" +
            "                      \"dynamicValue\": null\n" +
            "                    }\n" +
            "                  }\n" +
            "                },\n" +
            "                {\n" +
            "                  \"key\": {\n" +
            "                    \"type\": \"TIME_SERIES\",\n" +
            "                    \"key\": \"temperature\"\n" +
            "                  },\n" +
            "                  \"valueType\": \"NUMERIC\",\n" +
            "                  \"value\": null,\n" +
            "                  \"predicate\": {\n" +
            "                    \"type\": \"NUMERIC\",\n" +
            "                    \"operation\": \"GREATER\",\n" +
            "                    \"value\": {\n" +
            "                      \"defaultValue\": 25,\n" +
            "                      \"userValue\": null,\n" +
            "                      \"dynamicValue\": {\n" +
            "                        \"sourceType\": \"CURRENT_DEVICE\",\n" +
            "                        \"sourceAttribute\": \"temperatureAlarmThreshold\",\n" +
            "                        \"inherit\": false\n" +
            "                      }\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              ],\n" +
            "              \"spec\": {\n" +
            "                \"type\": \"SIMPLE\"\n" +
            "              }\n" +
            "            },\n" +
            "            \"schedule\": null,\n" +
            "            \"alarmDetails\": \"Current temperature = ${temperature}\",\n" +
            "            \"dashboardId\": null\n" +
            "          }\n" +
            "        },\n" +
            "        \"clearRule\": {\n" +
            "          \"condition\": {\n" +
            "            \"condition\": [\n" +
            "              {\n" +
            "                \"key\": {\n" +
            "                  \"type\": \"TIME_SERIES\",\n" +
            "                  \"key\": \"temperature\"\n" +
            "                },\n" +
            "                \"valueType\": \"NUMERIC\",\n" +
            "                \"value\": null,\n" +
            "                \"predicate\": {\n" +
            "                  \"type\": \"NUMERIC\",\n" +
            "                  \"operation\": \"LESS_OR_EQUAL\",\n" +
            "                  \"value\": {\n" +
            "                    \"defaultValue\": 25,\n" +
            "                    \"userValue\": null,\n" +
            "                    \"dynamicValue\": {\n" +
            "                      \"sourceType\": \"CURRENT_DEVICE\",\n" +
            "                      \"sourceAttribute\": \"temperatureAlarmThreshold\",\n" +
            "                      \"inherit\": false\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            ],\n" +
            "            \"spec\": {\n" +
            "              \"type\": \"SIMPLE\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"schedule\": null,\n" +
            "          \"alarmDetails\": \"Current temperature = ${temperature}\",\n" +
            "          \"dashboardId\": null\n" +
            "        },\n" +
            "        \"propagate\": false,\n" +
            "        \"propagateToOwner\": false,\n" +
            "        \"propagateToTenant\": false,\n" +
            "        \"propagateRelationTypes\": null\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": \"lowHumidityAlarmID\",\n" +
            "        \"alarmType\": \"Low Humidity\",\n" +
            "        \"createRules\": {\n" +
            "          \"MINOR\": {\n" +
            "            \"condition\": {\n" +
            "              \"condition\": [\n" +
            "                {\n" +
            "                  \"key\": {\n" +
            "                    \"type\": \"ATTRIBUTE\",\n" +
            "                    \"key\": \"humidityAlarmFlag\"\n" +
            "                  },\n" +
            "                  \"valueType\": \"BOOLEAN\",\n" +
            "                  \"value\": null,\n" +
            "                  \"predicate\": {\n" +
            "                    \"type\": \"BOOLEAN\",\n" +
            "                    \"operation\": \"EQUAL\",\n" +
            "                    \"value\": {\n" +
            "                      \"defaultValue\": true,\n" +
            "                      \"userValue\": null,\n" +
            "                      \"dynamicValue\": null\n" +
            "                    }\n" +
            "                  }\n" +
            "                },\n" +
            "                {\n" +
            "                  \"key\": {\n" +
            "                    \"type\": \"TIME_SERIES\",\n" +
            "                    \"key\": \"humidity\"\n" +
            "                  },\n" +
            "                  \"valueType\": \"NUMERIC\",\n" +
            "                  \"value\": null,\n" +
            "                  \"predicate\": {\n" +
            "                    \"type\": \"NUMERIC\",\n" +
            "                    \"operation\": \"LESS\",\n" +
            "                    \"value\": {\n" +
            "                      \"defaultValue\": 60,\n" +
            "                      \"userValue\": null,\n" +
            "                      \"dynamicValue\": {\n" +
            "                        \"sourceType\": \"CURRENT_DEVICE\",\n" +
            "                        \"sourceAttribute\": \"humidityAlarmThreshold\",\n" +
            "                        \"inherit\": false\n" +
            "                      }\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              ],\n" +
            "              \"spec\": {\n" +
            "                \"type\": \"SIMPLE\"\n" +
            "              }\n" +
            "            },\n" +
            "            \"schedule\": null,\n" +
            "            \"alarmDetails\": \"Current humidity = ${humidity}\",\n" +
            "            \"dashboardId\": null\n" +
            "          }\n" +
            "        },\n" +
            "        \"clearRule\": {\n" +
            "          \"condition\": {\n" +
            "            \"condition\": [\n" +
            "              {\n" +
            "                \"key\": {\n" +
            "                  \"type\": \"TIME_SERIES\",\n" +
            "                  \"key\": \"humidity\"\n" +
            "                },\n" +
            "                \"valueType\": \"NUMERIC\",\n" +
            "                \"value\": null,\n" +
            "                \"predicate\": {\n" +
            "                  \"type\": \"NUMERIC\",\n" +
            "                  \"operation\": \"GREATER_OR_EQUAL\",\n" +
            "                  \"value\": {\n" +
            "                    \"defaultValue\": 60,\n" +
            "                    \"userValue\": null,\n" +
            "                    \"dynamicValue\": {\n" +
            "                      \"sourceType\": \"CURRENT_DEVICE\",\n" +
            "                      \"sourceAttribute\": \"humidityAlarmThreshold\",\n" +
            "                      \"inherit\": false\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            ],\n" +
            "            \"spec\": {\n" +
            "              \"type\": \"SIMPLE\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"schedule\": null,\n" +
            "          \"alarmDetails\": \"Current humidity = ${humidity}\",\n" +
            "          \"dashboardId\": null\n" +
            "        },\n" +
            "        \"propagate\": false,\n" +
            "        \"propagateToOwner\": false,\n" +
            "        \"propagateToTenant\": false,\n" +
            "        \"propagateRelationTypes\": null\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"provisionDeviceKey\": null,\n" +
            "  \"firmwareId\": null,\n" +
            "  \"softwareId\": null,\n" +
            "  \"defaultEdgeRuleChainId\": null,\n" +
            "  \"externalId\": null,\n" +
            "  \"default\": false\n" +
            "}";

    private static DeviceProfile dp;

    @BeforeAll
    public static void before() {
        dp = JacksonUtil.fromString(DEVICE_PROFILE_STR, DeviceProfile.class);
    }

    @Ignore
    @Test
    void testDecode() {
        // warmup
        testDecode("FST", FSTUtils::encode);
        testDecode("Jackson", DataDecodingEncodingServiceTest::serializeJackson);
        testDecode("Default", SerializationUtils::serialize);

        testEncode("FST", FSTUtils.encode(dp), FSTUtils::decode);
        testEncode("Jackson", serializeJackson(dp), data -> JacksonUtil.fromBytes(data, DeviceProfile.class));
        testEncode("Default", serializeDefault(dp), data -> (DeviceProfile) SerializationUtils.deserialize(data));
    }

    private void testDecode(String methodName, Function<DeviceProfile, byte[]> serializeFunction) {
        // WARMUP;
        for (int i = 0; i < 1000; i++) {
            dp.setName(Integer.toString(i));
            serializeFunction.apply(dp);
        }
        long startTs = System.nanoTime();
        for (int i = 0; i < _1M; i++) {
            dp.setName(Integer.toString(i));
            serializeFunction.apply(dp);
        }
        long elapsedTime = System.nanoTime() - startTs;
        System.out.println("S: " + methodName + " SIZE: " + serializeFunction.apply(dp).length + " TIME: " + elapsedTime + "ns or " + (elapsedTime / 1000000) + "ms");
    }

    private void testEncode(String methodName, byte[] data, Function<byte[], DeviceProfile> deserializeFunction) {
        // WARMUP;
        for (int i = 0; i < 1000; i++) {
            dp.setName(Integer.toString(i));
            deserializeFunction.apply(data);
        }
        long startTs = System.nanoTime();
        for (int i = 0; i < _1M; i++) {
            dp.setName(Integer.toString(i));
            deserializeFunction.apply(data);
        }
        long elapsedTime = System.nanoTime() - startTs;
        System.out.println("D: " +methodName + " TIME: " + elapsedTime + "ns or " + (elapsedTime / 1000000) + "ms");
    }

    static <T> byte[] serializeDefault(T object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    static <T> byte[] serializeJackson(T object) {
        return JacksonUtil.toString(object).getBytes(StandardCharsets.UTF_8);
    }

}
