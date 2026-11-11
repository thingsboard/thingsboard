// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { MessageType } from '@shared/models/rule-node.models';
import { isArray, isDefinedAndNotNull, isObject } from '@core/utils';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';

export interface EmailConfig {
  from: string;
  to: string;
  cc?: string;
  bcc?: string;
  subject: string;
  body: string;
}

export const sendRPCRequestDefaults: SchedulerEventConfiguration = {
  msgType: MessageType.RPC_CALL_FROM_SERVER_TO_DEVICE,
  originatorId: null,
  msgBody: {
    method: null,
    params: null
  },
  metadata: {
    oneway: true,
    timeout: 5000,
    persistent: false
  }
};

export const updateAttributesDefaults: SchedulerEventConfiguration = {
  msgType: MessageType.POST_ATTRIBUTES_REQUEST,
  originatorId:  null,
  msgBody: {},
  metadata: {
    scope: null
  }
};

export const defaultEmailConfig: EmailConfig =  {
  from: null,
  to: null,
  subject: 'Report generated on %d{yyyy-MM-dd HH:mm:ss}',
  body: 'Report was successfully generated on %d{yyyy-MM-dd HH:mm:ss}.\nSee attached report file.'
};

export const safeMerge = <T>(defaults: T | { [key: string]: any },
                          value: Partial<T> | { [key: string]: any } | null): T => {
  const result = {...defaults};

  if (value) {
    for (const key in value) {
      if (value.hasOwnProperty(key)) {
        const valueToUpdate = value[key];
        if (isDefinedAndNotNull(valueToUpdate)) {
          if (isObject(valueToUpdate) && !isArray(valueToUpdate)) {
            result[key] = safeMerge(result[key], valueToUpdate);
          } else {
            result[key] = valueToUpdate;
          }
        }
      }
    }
  }

  return result as T;
};
