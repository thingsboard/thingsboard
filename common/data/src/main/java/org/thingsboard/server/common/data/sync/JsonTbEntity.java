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
package org.thingsboard.server.common.data.sync;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "entityType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
@JsonSubTypes({
        @Type(name = "DEVICE", value = Device.class),
        @Type(name = "RULE_CHAIN", value = RuleChain.class),
        @Type(name = "DEVICE_PROFILE", value = DeviceProfile.class),
        @Type(name = "ASSET_PROFILE", value = AssetProfile.class),
        @Type(name = "ASSET", value = Asset.class),
        @Type(name = "DASHBOARD", value = Dashboard.class),
        @Type(name = "CUSTOMER", value = Customer.class),
        @Type(name = "ENTITY_VIEW", value = EntityView.class),
        @Type(name = "WIDGETS_BUNDLE", value = WidgetsBundle.class),
        @Type(name = "WIDGET_TYPE", value = WidgetTypeDetails.class),
        @Type(name = "NOTIFICATION_TEMPLATE", value = NotificationTemplate.class),
        @Type(name = "NOTIFICATION_TARGET", value = NotificationTarget.class),
        @Type(name = "NOTIFICATION_RULE", value = NotificationRule.class),
        @Type(name = "TB_RESOURCE", value = TbResource.class),
        @Type(name = "OTA_PACKAGE", value = OtaPackage.class),
        @Type(name = "AI_MODEL", value = AiModel.class)
})
@JsonIgnoreProperties(value = {"tenantId", "createdTime", "version"}, ignoreUnknown = true)
public @interface JsonTbEntity {}
