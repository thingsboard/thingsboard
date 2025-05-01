/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.smartscene;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.firebase.messaging.FirebaseMessagingException;
import io.swagger.v3.core.util.Json;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
//import org.thingsboard.server.dao.model.sql.SmartSceneEntity;
//import org.thingsboard.server.dao.sql.smartscene.SmartSceneRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.firebase.FirebaseService;
import org.thingsboard.server.service.scheduler.WeatherZuno;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SmartSceneService {
//    protected final Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
//    private final SmartSceneRepository smartSceneRepository;
//    private final TelemetrySubscriptionService telemetrySubscriptionService;
//    private final FirebaseService firebaseService;
//    private final TimeseriesService timeseriesService;
//    private final AttributesService attributesService;
//
//    public SmartSceneService(SmartSceneRepository smartSceneRepository, TelemetrySubscriptionService telemetrySubscriptionService, FirebaseService firebaseService, TimeseriesService timeseriesService, AttributesService attributesService) {
//        this.smartSceneRepository = smartSceneRepository;
//        this.telemetrySubscriptionService = telemetrySubscriptionService;
//        this.firebaseService = firebaseService;
//        this.timeseriesService = timeseriesService;
//        this.attributesService = attributesService;
//    }
//
//    public SmartSceneEntity findById(long id) {
//        return smartSceneRepository.findById(id);
//    }
//
//    public SmartSceneEntity findByTitle(String title) {
//        return smartSceneRepository.findByTitle(title);
//    }
//
//    public SmartSceneEntity save(SmartSceneEntity smartSceneEntity) {
//        return smartSceneRepository.save(smartSceneEntity);
//    }
//
//    public List<SmartSceneEntity> findAll() {
//        return smartSceneRepository.findAll();
//    }
//
//    public List<SmartSceneEntity> findAllByTenantId(UUID tenantId) {
//        return smartSceneRepository.findAllByTenantId(tenantId);
//    }
//
//    public void runSmartScenes(UUID tenantId, ZonedDateTime currentTime, WeatherZuno currentWeather) throws JsonProcessingException, FirebaseMessagingException {
//        List<SmartSceneEntity> smartScenes = smartSceneRepository.findAllByTenantId(tenantId);
//        for (SmartSceneEntity smartScene : smartScenes) {
//            String script = smartScene.getScript();
//            Long id = smartScene.getId();
//            System.out.println("Checking script for Smart Scene: " + smartScene.getTitle());
//            executeScript(tenantId, id, script, currentTime, currentWeather);
//        }
//    }
//
//    private void executeScript(UUID tenantId, Long id, String script, ZonedDateTime currentTime, WeatherZuno currentWeather) throws JsonProcessingException, FirebaseMessagingException {
//        // Implement the logic to execute the script here
//        /*
//      {
//  "type": "SMART_SCENE",
//  "if": [
//    {
//      "type": "SCHEDULE",
//      "condition": {
//        "time": "00:00",
//        "days": [2, 3, 5]
//      }
//    },
//    {
//      "type": "WEATHER",
//      "condition": {
//        "temperature": {
//          "operator": ">",
//          "value": 20
//        }
//      }
//    },
//    {
//      "type": "DEVICE",
//      "condition": {
//        "deviceId": "adbsadsa",
//        "telemetry": {
//          "temperature": {
//            "operator": "<",
//            "value": 30
//          }
//        },
//        "attributes": {
//          "pin_10": {
//            "operator": "=",
//            "value": 1
//          }
//        }
//      }
//    }
//  ],
//  "actions": [
//    {
//      "CMD": "PUSH_NOTIFY",
//      "params": [
//        {
//          "title": "Test",
//          "body": "Test"
//        }
//      ]
//    },
//    {
//      "CMD": "SET_OUTPUT",
//      "params": [
//        {
//          "deviceId": "adbsadsa",
//          "pin_10": 1
//        }
//      ]
//    }
//  ]
//}
//
//         */
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode root = mapper.readTree(script);
//        JsonNode ifConditions = root.path("if");
//        JsonNode actions = root.path("actions");
//
//        boolean checkCondition = true;
//
//        for (JsonNode condition : ifConditions) {
//            String type = condition.path("type").asText();
//            if (type.equals("SCHEDULE")) {
//                JsonNode scheduleCondition = condition.path("condition");
//                String time = scheduleCondition.path("time").asText();
//                List<Integer> days = mapper.convertValue(scheduleCondition.path("days"), List.class);
//                checkCondition = checkTime(time, days, currentTime);
//            } else if (type.equals("WEATHER")) {
//                JsonNode weatherCondition = condition.path("condition");
//                checkCondition = checkWeather(weatherCondition, currentWeather, currentTime);
//            } else if (type.equals("DEVICE")) {
//                JsonNode deviceCondition = condition.path("condition");
//                String deviceId = deviceCondition.path("deviceId").asText();
//
//                if (deviceCondition.has("telemetry")) {
//                    JsonNode telemetryCondition = deviceCondition.path("telemetry");
//                    for (JsonNode telemetry : telemetryCondition) {
//                        String key = telemetry.path("key").asText();
//                        String operator = telemetry.path("operator").asText();
//                        String value = telemetry.path("value").asText();
//                        // Implement the logic to check the telemetry condition
//                        checkCondition = checkTelemetry(tenantId, deviceId, key, operator, value);
//                    }
//                    System.out.println("Check telemetry condition: " + checkCondition);
//                } else if (deviceCondition.has("attributes")) {
//                    JsonNode attributesCondition = deviceCondition.path("attributes");
//                    for (JsonNode attribute : attributesCondition) {
//                        String key = attribute.path("key").asText();
//                        String operator = attribute.path("operator").asText();
//                        String value = attribute.path("value").asText();
//                        // Implement the logic to check the attributes condition
//                        checkCondition = checkAttribute(tenantId, deviceId, key, operator, value);
//                    }
//                    System.out.println("Check attributes condition: " + checkCondition);
//                }
//            }
//            System.out.println("Check Condition " + type + " : " + checkCondition);
//            if (!checkCondition) {
//                return;
//            }
//        }
//
//        System.out.println("Executing actions");
//        for (JsonNode action : actions) {
//            String cmd = action.path("CMD").asText();
//            JsonNode params = action.path("params");
//            if (cmd.equals("PUSH_NOTIFY")) {
//                for (JsonNode param : params) {
//                    String title = param.path("title").asText();
//                    String body = param.path("body").asText();
//                    String deviceToken = param.path("fmcToken").asText();
//                    long time = param.path("time").asLong();
//                    long lastTime = param.path("lastTime").asLong();
//                    if (System.currentTimeMillis() - lastTime > time) {
//                        firebaseService.sendNotification(deviceToken, title, body);
//                    }
//                }
//            } else if (cmd.equals("SET_OUTPUT")) {
//                for (JsonNode param : params) {
//                    String deviceId = param.path("deviceId").asText();
//                    int pin = param.path("pin").asInt();
//                    int value = param.path("value").asInt();
//                    setOutputOfDeviceId(tenantId, deviceId, pin, value);
//                }
//            }
//        }
//    }
//
//    private boolean checkTelemetry(UUID tenantId ,String deviceId, String key, String operator, String value) {
//        Optional<TsKvEntry> tsKvEntry = Optional.empty();
//        try {
//            tsKvEntry = timeseriesService.findLatest(
//                    TenantId.fromUUID(tenantId),
//                    EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceId),
//                    key
//            ).get();
//        } catch (Exception e) {
//            log.error("Error while checking telemetry for deviceId: " + deviceId, e);
//            return false;
//        }
//
//        if (tsKvEntry.isPresent()) {
//            TsKvEntry entry = tsKvEntry.get();
//            String dataType = entry.getDataType().name(); // Lấy kiểu dữ liệu: STRING, DOUBLE, etc.
//            System.out.println("DataType: " + dataType);
//
//            try {
//                switch (dataType) {
//                    case "STRING" -> {
//                        String val = entry.getStrValue().orElse(null);
//                        if (val == null) return false;
//                        return switch (operator) {
//                            case "=" -> val.equals(value);
//                            default -> false; // không so sánh được >, < với chuỗi
//                        };
//                    }
//                    case "LONG" -> {
//                        long val = entry.getLongValue().orElse(0L);
//                        long cmp = Long.parseLong(value);
//                        return switch (operator) {
//                            case "=" -> val == cmp;
//                            case ">" -> val > cmp;
//                            case "<" -> val < cmp;
//                            case ">=" -> val >= cmp;
//                            case "<=" -> val <= cmp;
//                            default -> false;
//                        };
//                    }
//                    case "DOUBLE" -> {
//                        double val = entry.getDoubleValue().orElse(0.0);
//                        double cmp = Double.parseDouble(value);
//                        return switch (operator) {
//                            case "=" -> val == cmp;
//                            case ">" -> val > cmp;
//                            case "<" -> val < cmp;
//                            case ">=" -> val >= cmp;
//                            case "<=" -> val <= cmp;
//                            default -> false;
//                        };
//                    }
//                    case "BOOLEAN" -> {
//                        boolean val = entry.getBooleanValue().orElse(false);
//                        boolean cmp = Boolean.parseBoolean(value);
//                        return operator.equals("=") && val == cmp;
//                    }
//                    default -> {
//                        return false;
//                    }
//                }
//            } catch (Exception e) {
//                log.error("Error while comparing telemetry value", e);
//                return false;
//            }
//        }
//
//
//        return false;
//    }
//
//    private boolean checkAttribute(UUID tenantId, String deviceId, String key, String operator, String value) {
//        Optional<AttributeKvEntry> attributeKvEntry = Optional.empty();
//        try {
//            attributeKvEntry = attributesService.find(
//                    TenantId.fromUUID(tenantId),
//                    EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceId),
//                    AttributeScope.SHARED_SCOPE,
//                    key
//            ).get();
//        } catch (Exception e) {
//            log.error("Error while checking attribute for deviceId: " + deviceId, e);
//            return false;
//        }
//
//        if (attributeKvEntry.isPresent()) {
//            AttributeKvEntry attributeEntry = attributeKvEntry.get();
//            String dataType = attributeEntry.getDataType().name(); // Lấy kiểu dữ liệu
//            System.out.println("Check attribute " + key + " of type " + dataType + " with operator " + operator + " and value " + value);
//
//            try {
//                switch (dataType) {
//                    case "STRING" -> {
//                        String attrVal = attributeEntry.getStrValue().orElse(null);
//                        if (attrVal == null) return false;
//                        return switch (operator) {
//                            case "=" -> attrVal.equals(value);
//                            case "!=" -> !attrVal.equals(value);
//                            default -> false; // Không hỗ trợ > < >= <= cho String
//                        };
//                    }
//                    case "LONG" -> {
//                        long attrVal = attributeEntry.getLongValue().orElse(0L);
//                        long cmp = Long.parseLong(value);
//                        return switch (operator) {
//                            case "=" -> attrVal == cmp;
//                            case "!=" -> attrVal != cmp;
//                            case ">" -> attrVal > cmp;
//                            case "<" -> attrVal < cmp;
//                            case ">=" -> attrVal >= cmp;
//                            case "<=" -> attrVal <= cmp;
//                            default -> false;
//                        };
//                    }
//                    case "DOUBLE" -> {
//                        double attrVal = attributeEntry.getDoubleValue().orElse(0.0);
//                        double cmp = Double.parseDouble(value);
//                        return switch (operator) {
//                            case "=" -> attrVal == cmp;
//                            case "!=" -> attrVal != cmp;
//                            case ">" -> attrVal > cmp;
//                            case "<" -> attrVal < cmp;
//                            case ">=" -> attrVal >= cmp;
//                            case "<=" -> attrVal <= cmp;
//                            default -> false;
//                        };
//                    }
//                    case "BOOLEAN" -> {
//                        boolean attrVal = attributeEntry.getBooleanValue().orElse(false);
//                        boolean cmp = Boolean.parseBoolean(value);
//                        return switch (operator) {
//                            case "=" -> attrVal == cmp;
//                            case "!=" -> attrVal != cmp;
//                            default -> false;
//                        };
//                    }
//                    default -> {
//                        return false;
//                    }
//                }
//            } catch (Exception e) {
//                log.error("Error while comparing attribute value", e);
//                return false;
//            }
//        }
//
//
//        return false;
//    }
//
//    private boolean checkTime(String time, List<Integer> days, ZonedDateTime currentTime) {
//        String[] timeParts = time.split(":");
//        int hour = Integer.parseInt(timeParts[0]);
//        int minute = Integer.parseInt(timeParts[1]);
//        boolean isTimeMatch = (currentTime.getHour() == hour && currentTime.getMinute() == minute);
//        boolean isDayMatch = days.contains(currentTime.getDayOfWeek().getValue());
//        return isTimeMatch && isDayMatch;
//    }
//
//    private boolean checkWeather(JsonNode weatherCondition, WeatherZuno currentWeather, ZonedDateTime currentTime) {
//        if(weatherCondition.has("temperature")){
//            JsonNode condition = weatherCondition.path("temperature");
//            String operator = condition.path("operator").asText();
//            double value = condition.path("value").asDouble();
//            switch (operator) {
//                case "=" -> {
//                    return currentWeather.getTemp() == value;
//                }
//                case ">" -> {
//                    return currentWeather.getTemp() > value;
//                }
//                case "<" -> {
//                    return currentWeather.getTemp() < value;
//                }
//                case ">=" -> {
//                    return currentWeather.getTemp() >= value;
//                }
//                case "<=" -> {
//                    return currentWeather.getTemp() <= value;
//                }
//            }
//        } else if(weatherCondition.has("humidity")) {
//            JsonNode condition = weatherCondition.path("humidity");
//            String operator = condition.path("operator").asText();
//            double value = condition.path("value").asDouble();
//            switch (operator) {
//                case "=" -> {
//                    return currentWeather.getHumidity() == value;
//                }
//                case ">" -> {
//                    return currentWeather.getHumidity() > value;
//                }
//                case "<" -> {
//                    return currentWeather.getHumidity() < value;
//                }
//                case ">=" -> {
//                    return currentWeather.getHumidity() >= value;
//                }
//                case "<=" -> {
//                    return currentWeather.getHumidity() <= value;
//                }
//            }
//        } else if(weatherCondition.has("wind_speed")) {
//            JsonNode condition = weatherCondition.path("wind_speed");
//            String operator = condition.path("operator").asText();
//            double value = condition.path("value").asDouble();
//            switch (operator) {
//                case "=" -> {
//                    return currentWeather.getWind_speed() == value;
//                }
//                case ">" -> {
//                    return currentWeather.getWind_speed() > value;
//                }
//                case "<" -> {
//                    return currentWeather.getWind_speed() < value;
//                }
//                case ">=" -> {
//                    return currentWeather.getWind_speed() >= value;
//                }
//                case "<=" -> {
//                    return currentWeather.getWind_speed() <= value;
//                }
//            }
//        }
//        else if(weatherCondition.has("weather")) {
//            JsonNode condition = weatherCondition.path("weather");
//            String operator = condition.path("operator").asText();
//            String value = condition.path("value").asText();
//            switch (operator) {
//                case "=" -> {
//                    return currentWeather.getWeathers().get(0).getMain().equals(value);
//                }
//                case "!=" -> {
//                    return !currentWeather.getWeathers().get(0).getMain().equals(value);
//                }
//            }
//        } // neu truoc binh minh bao nhieu phut
//        else if(weatherCondition.has("sunrise")) {
//            JsonNode condition = weatherCondition.path("sunrise");
//            String operator = condition.path("operator").asText();
//            int value = condition.path("value").asInt();
//            switch (operator) {
//                case "back" -> {
//                    return (currentWeather.getSunrise() - value) / (1000 * 60) ==  currentTime.getMinute();
//                }
//                case "front" -> {
//                    return (currentWeather.getSunrise() + value) / (1000 * 60) == currentTime.getMinute();
//                }
//            }
//        } else if(weatherCondition.has("sunset")) {
//            JsonNode condition = weatherCondition.path("sunset");
//            String operator = condition.path("operator").asText();
//            int value = condition.path("value").asInt();
//            switch (operator) {
//                case "back" -> {
//                    return (currentWeather.getSunset() - value) / (1000 * 60) == currentTime.getMinute();
//                }
//                case "front" -> {
//                    return (currentWeather.getSunset() + value) / (1000 * 60) == currentTime.getMinute();
//                }
//            }
//        }
//        return false;
//    }
//
//    private void setOutputOfDeviceId(UUID tenantId, String deviceId, int pin, int value) {
//        System.out.println("Set output of deviceId " + deviceId + " pin " + pin + " value " + value);
//        EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceId);
//        telemetrySubscriptionService.saveAttributes(
//                AttributesSaveRequest.builder()
//                        .tenantId(TenantId.fromUUID(tenantId))
//                        .entityId(entityId)
//                        .scope(AttributeScope.SHARED_SCOPE)
//                        .entries(
//                                List.of(
//                                        new BaseAttributeKvEntry(new StringDataEntry("pin_" + String.valueOf(pin), String.valueOf(value)), System.currentTimeMillis())
//                                )
//                        )
//                        .callback(new FutureCallback<>() {
//                            @Override
//                            public void onSuccess(@Nullable Void result) {
//                                log.info("Attributes saved and MQTT event published for device {}", deviceId);
//                            }
//                            @Override
//                            public void onFailure(Throwable t) {
//                                log.error("Failed to publish MQTT event for device {}", deviceId, t);
//                            }
//                        })
//                        .build()
//        );
//    }
}
