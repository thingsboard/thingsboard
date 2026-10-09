// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { UntypedFormControl, Validators } from '@angular/forms';
import { IntegrationType, MqttQos, MqttTopicFilter } from '@shared/models/integration.models';

export enum ThingsStartHostType {
  Region = 0,
  Custom = 1
}

export const ThingsStartHostTypeTranslation = new Map<ThingsStartHostType, string> ([
  [ThingsStartHostType.Region, 'Region'],
  [ThingsStartHostType.Custom, 'Custom'],
]);

const PRIVATE_NETWORK_REGEXP = /^((http|https|pulsar):\/\/)?(127\.|(10\.)|(172\.1[6-9]\.)|(172\.2[0-9]\.)|(172\.3[0-1]\.)|(192\.168\.)|localhost(:[0-9]+)?$)/;

export enum ttnVersion {
  v2,
  v3
}

export interface TtnVersionParameter {
  downlinkPattern: string;
  uplinkTopic: MqttTopicFilter[];
}

export const ttnVersionMap = new Map<ttnVersion, TtnVersionParameter>([
  [
    ttnVersion.v2, {
      downlinkPattern: '${applicationId}/devices/${devId}/down',
      uplinkTopic: [{
        filter: '+/devices/+/up',
        qos: MqttQos.AT_MOST_ONE
      }]
    }
  ],
  [
    ttnVersion.v3, {
    downlinkPattern: 'v3/${applicationId}/devices/${devId}/down/push',
    uplinkTopic: [{
      filter: 'v3/+/devices/+/up',
      qos: MqttQos.AT_MOST_ONE
    }]
  }
  ]
]);

export function integrationEndPointUrl(type: IntegrationType, baseUrl: string, key = ''): string {
  return `${baseUrl}/api/v1/integrations/${type.toLowerCase()}/${key}`;
}

export function privateNetworkAddressValidator(control: UntypedFormControl): { [key: string]: any } | null {
  if (control.value) {
    const host = control.value.trim();
    return !PRIVATE_NETWORK_REGEXP.test(host) ? null : {
      privateNetwork: {
        valid: false
      }
    };
  }
  return null;
}
