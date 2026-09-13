// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Same as @TbMqttTransportComponent with additional condition by `transport.mqtt.ssl.enabled == true`
 */

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${transport.mqtt.ssl.enabled:false}'=='true' && ('${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.mqtt.enabled:true}'=='true'))")
public @interface TbMqttSslTransportComponent {}
