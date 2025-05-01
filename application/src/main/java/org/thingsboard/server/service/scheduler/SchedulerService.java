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
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.firebase.messaging.FirebaseMessagingException;
import io.swagger.v3.core.util.Json;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.model.sql.ScheduleZunoEntity;
import org.thingsboard.server.dao.sql.scheduler.ScheduleZunoRepository;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.smartscene.SmartSceneService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.management.Attribute;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.controller.BaseController.toException;

@Service
@Slf4j
public class SchedulerService  {

    private final ScheduleZunoRepository scheduleZunoRepository;
    private final TelemetrySubscriptionService telemetrySubscriptionService;
    private final AssetService assetService;
    private final EntityActionService entityActionService;

    private final TbLogEntityActionService logEntityActionService;

    public SchedulerService(ScheduleZunoRepository scheduleZunoRepository, TelemetrySubscriptionService telemetrySubscriptionService, SmartSceneService smartSceneService, AssetService assetService, EntityActionService entityActionService, TbLogEntityActionService logEntityActionService) {
        this.scheduleZunoRepository = scheduleZunoRepository;
        this.telemetrySubscriptionService = telemetrySubscriptionService;
        this.assetService = assetService;
        this.entityActionService = entityActionService;
        this.logEntityActionService = logEntityActionService;
    }


    // Chạy mỗi 60 giây tính từ giây thứ 0
    @Scheduled(cron = "0 0/1 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendCurrentTimeToAllAssets() throws JsonProcessingException, FirebaseMessagingException {
        System.out.println("Updated currentTime");
        List<AttributeKvEntry> attributes = List.of(
                new BaseAttributeKvEntry(new LongDataEntry("activeTime", System.currentTimeMillis()), System.currentTimeMillis())
        );
        List<ScheduleZunoEntity> scheduleZunoEntities = scheduleZunoRepository.findAll();
        for (ScheduleZunoEntity scheduleZunoEntity : scheduleZunoEntities) {
            UUID tenantId = scheduleZunoEntity.getTenantId();
            String title = scheduleZunoEntity.getTitle();
            String action = scheduleZunoEntity.getAction();
            Long repeatInterval = scheduleZunoEntity.getRepeatInterval();
            ZonedDateTime lastRunTime = scheduleZunoEntity.getLastRunTime();
            ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
            System.out.println("ScheduleZunoEntity: " + scheduleZunoEntity);
            // kiem tra xem thoi gian hien tai co phai la thoi gian lap lai khong
//            if (currentTime.isAfter(lastRunTime) && currentTime.isBefore(lastRunTime.plusSeconds(repeatInterval))) {
                System.out.println("Time to run: " + title);
                // duyet cac asset cua tenant nay
                PageLink pageLink = new PageLink(10);
                do {
                    List<Asset> assets = assetService.findAssetsByTenantId(TenantId.fromUUID(tenantId), pageLink).getData();
                    for (Asset asset : assets) {
                        telemetrySubscriptionService.saveAttributes(AttributesSaveRequest.builder()
                                .tenantId(TenantId.fromUUID(tenantId))
                                .entityId(asset.getId())
                                .scope(AttributeScope.SERVER_SCOPE)
                                .entries(attributes)
                                .callback(new FutureCallback<>() {
                                    @Override
                                    public void onSuccess(@Nullable Void tmp) {
                                        entityActionService.pushEntityActionToRuleEngine(
                                                asset.getId(),
                                                null,
                                                TenantId.fromUUID(tenantId),
                                                null,
                                                ActionType.ATTRIBUTES_UPDATED,
                                                null,
                                                AttributeScope.SHARED_SCOPE,
                                                attributes
                                        );
                                        // get current user with tenantId
//                                        SecurityUser user = new SecurityUser();
//                                        user.setTenantId(TenantId.fromUUID(tenantId));
//                                        user.setId(UserId.fromString("6140ed70-0211-11f0-87b6-9f1de160d4f5"));
//                                        user.setAuthority(Authority.TENANT_ADMIN);
//                                        user.setSessionId(UUID.randomUUID().toString());
//                                        user.setEnabled(true);
//                                        logAttributesUpdated(user, asset.getId(), AttributeScope.SERVER_SCOPE,
//                                                List.of(new BaseAttributeKvEntry(new StringDataEntry("callTimex", currentTime.toString()), System.currentTimeMillis())), null);
                                        log.info("Attributes saved and MQTT event published for device {}", asset.getId());
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        log.error("Failed to save attributes for device {}: {}", asset.getId(), t.getMessage());
                                    }
                                })
                                .build());
                    }
                    if(assets.size() < pageLink.getPageSize()) {
                        break;
                    }
                    pageLink = pageLink.nextPageLink();
                } while(true);
//            }
        }
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes, Throwable e) {
        System.out.println("Log attributes updated: " + user.getTenantId());
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_UPDATED, user,
                toException(e), scope, attributes);
    }
}

//    // chay moi 10 phut
//    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
//    public void updateWeather() throws JsonProcessingException {
//        System.out.println("Update weather");
////        currentWeather = getCurrentWeather();
//        // Save the current weather to the database or perform any other necessary actions
//    }

//    public void runSmartScenes(UUID tenantId, ZonedDateTime currentTime, WeatherZuno currentWeather) throws JsonProcessingException, FirebaseMessagingException {
//        smartSceneService.runSmartScenes(tenantId, currentTime, currentWeather);
//    }

    // call api de lay thoi tiet https://api.openweathermap.org/data/3.0/onecall?lat=21.0294498&lon=105.8544441&appid=0836fe29ccb5ac2cbb4c217b56f1d895
//    public WeatherZuno getCurrentWeather() throws JsonProcessingException {
//        RestTemplate restTemplate = new RestTemplate();
//        String json = restTemplate.getForObject("https://api.openweathermap.org/data/3.0/onecall?lat=21.0294498&lon=105.8544441&appid=0836fe29ccb5ac2cbb4c217b56f1d895&lang=vi&exclude=minutely,hourly,daily,alerts&units=metric", String.class);
//        System.out.println(json);
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(json);
//
//        WeatherZuno weather = objectMapper.convertValue(jsonNode.get("current"), WeatherZuno.class);
//
//        return weather;
//    }