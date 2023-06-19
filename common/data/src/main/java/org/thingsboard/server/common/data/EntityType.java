/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public enum EntityType {
    TENANT,
    CUSTOMER,
    USER,
    DASHBOARD,
    ASSET,
    DEVICE,
    ALARM,
    RULE_CHAIN,
    RULE_NODE,
    ENTITY_VIEW,
    WIDGETS_BUNDLE,
    WIDGET_TYPE,
    TENANT_PROFILE,
    DEVICE_PROFILE,
    ASSET_PROFILE,
    API_USAGE_STATE,
    TB_RESOURCE,
    OTA_PACKAGE,
    EDGE,
    RPC,
    QUEUE,
    NOTIFICATION_TARGET,
    NOTIFICATION_TEMPLATE,
    NOTIFICATION_REQUEST,
    NOTIFICATION,
    NOTIFICATION_RULE;


    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

}
