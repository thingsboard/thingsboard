// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export enum MqttVersion {
  MQTT_3_1 = 'MQTT_3_1',
  MQTT_3_1_1 = 'MQTT_3_1_1',
  MQTT_5 = 'MQTT_5'
}

export const DEFAULT_MQTT_VERSION = MqttVersion.MQTT_3_1_1;

export const MqttVersionTranslation = new Map<MqttVersion, string>([
  [MqttVersion.MQTT_3_1, 'MQTT 3.1'],
  [MqttVersion.MQTT_3_1_1, 'MQTT 3.1.1'],
  [MqttVersion.MQTT_5, 'MQTT 5.0']
]);
