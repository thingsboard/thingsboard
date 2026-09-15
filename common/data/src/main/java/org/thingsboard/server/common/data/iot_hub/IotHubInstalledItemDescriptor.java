// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @Type(name = "WIDGET", value = WidgetInstalledItemDescriptor.class),
        @Type(name = "DASHBOARD", value = DashboardInstalledItemDescriptor.class),
        @Type(name = "CALCULATED_FIELD", value = CalculatedFieldInstalledItemDescriptor.class),
        @Type(name = "ALARM_RULE", value = AlarmRuleInstalledItemDescriptor.class),
        @Type(name = "RULE_CHAIN", value = RuleChainInstalledItemDescriptor.class),
        @Type(name = "DEVICE", value = DeviceInstalledItemDescriptor.class),
        @Type(name = "SOLUTION_TEMPLATE", value = SolutionTemplateInstalledItemDescriptor.class)
})
public interface IotHubInstalledItemDescriptor extends Serializable {

}
