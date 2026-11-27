// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgZone } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';

export type AiCreditsLimitType = 'TENANT' | 'POOL';

export interface AiCreditsUpdate {
  count?: number;
  limit?: number;
  state?: string;
}

export const watchAiCredits = (telemetryWsService: TelemetryWebsocketService, zone: NgZone,
                               onUpdate: (update: AiCreditsUpdate) => void): TelemetrySubscriber => {
  const subscriber = TelemetrySubscriber.createEntityFilterLatestSubscription(
    telemetryWsService,
    {type: AliasFilterType.apiUsageState},
    zone,
    [
      {key: 'aiApiState', type: EntityKeyType.TIME_SERIES},
      {key: 'aiCreditsLimit', type: EntityKeyType.TIME_SERIES},
      {key: 'aiCreditsCount', type: EntityKeyType.TIME_SERIES}
    ]
  );
  subscriber.entityData$.subscribe(update => {
    const entityData = update.data?.data ?? update.update;
    const ts = entityData?.[0]?.latest?.[EntityKeyType.TIME_SERIES];
    if (ts) {
      onUpdate({
        count: ts.aiCreditsCount ? Number(ts.aiCreditsCount.value) || 0 : undefined,
        limit: ts.aiCreditsLimit ? Number(ts.aiCreditsLimit.value) || 0 : undefined,
        state: ts.aiApiState?.value
      });
    }
  });
  subscriber.subscribe();
  return subscriber;
};

export const aiCreditsResetDate = (): Date => {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 1));
};

export const aiCreditsLimitOf = (error: any): AiCreditsLimitType | null => {
  if (error?.status !== 429) {
    return null;
  }
  let body = error.error;
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch {
      return null;
    }
  }
  return body?.aiCreditsLimit === 'TENANT' ? 'TENANT' : null;
};
