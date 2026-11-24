// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, NgZone, signal } from '@angular/core';
import { UsageInfoService } from '@core/http/usage-info.service';
import { TourService } from '@core/services/tour.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { UsageInfo } from '@shared/models/usage.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { HomeToggleOption } from '@home/pages/home-page/components/home-toggle.component';

const CRITICAL_PERCENT = 90;

type UsageGroupId = 'entities' | 'api';

interface UsageRow {
  name: string;
  used: number;
  limit: number;
  percent: number;
  critical: boolean;
}

interface UsageGroup {
  id: UsageGroupId;
  label: string;
  rows: UsageRow[];
}

const ENTITY_ROWS: [string, keyof UsageInfo, keyof UsageInfo][] = [
  ['device.devices', 'devices', 'maxDevices'],
  ['asset.assets', 'assets', 'maxAssets'],
  ['user.users', 'users', 'maxUsers'],
  ['dashboard.dashboards', 'dashboards', 'maxDashboards'],
  ['customer.customers', 'customers', 'maxCustomers']
];

const API_ROWS: [string, string, string][] = [
  ['api-usage.transport-messages', 'transportMsgCount', 'transportMsgLimit'],
  ['api-usage.javascript', 'jsExecutionCount', 'jsExecutionLimit'],
  ['api-usage.alarms-created', 'createdAlarmsCount', 'createdAlarmsLimit'],
  ['api-usage.email', 'emailCount', 'emailLimit'],
  ['api-usage.sms', 'smsCount', 'smsLimit'],
  ['api-usage.ai-credits', 'aiCreditsCount', 'aiCreditsLimit']
];

@Component({
  selector: 'tb-api-usage-home',
  templateUrl: './api-usage-home.component.html',
  styleUrls: ['./api-usage-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApiUsageHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.USAGE];

  readonly tabOptions: HomeToggleOption<UsageGroupId>[] = [
    {value: 'entities', label: 'widgets.usage-info.entities'},
    {value: 'api', label: 'widgets.usage-info.api-calls'}
  ];

  private readonly apiRows = inject(TourService).aiAvailable() ? API_ROWS : API_ROWS.filter(([name]) => name !== 'api-usage.ai-credits');

  private readonly apiKeys = this.apiRows.flatMap(([, count, limit]) => [count, limit]);

  private readonly usage = signal<UsageInfo | null>(null);
  private readonly apiUsage = signal<{[key: string]: number} | null>(null);
  private tabChosen = false;

  readonly tab = signal<UsageGroupId>('entities');

  readonly groups = computed<UsageGroup[]>(() => {
    const usage = this.usage();
    const apiUsage = this.apiUsage();
    if (!usage || !apiUsage) {
      return [];
    }
    return [
      {
        id: 'entities',
        label: 'widgets.usage-info.entities',
        rows: ENTITY_ROWS.map(([name, used, limit]) => this.toRow(name, usage[used] as number, usage[limit] as number))
      },
      {
        id: 'api',
        label: 'widgets.usage-info.api-calls',
        rows: this.apiRows.map(([name, count, limit]) => this.toRow(name, apiUsage[count], apiUsage[limit]))
      }
    ];
  });

  constructor() {
    inject(UsageInfoService).getUsageInfo().subscribe(usage => {
      this.usage.set(usage);
      this.chooseTab();
    });
    const subscriber = this.subscribeApiUsage(inject(TelemetryWebsocketService), inject(NgZone));
    inject(DestroyRef).onDestroy(() => subscriber.unsubscribe());
  }

  private subscribeApiUsage(telemetryWsService: TelemetryWebsocketService, zone: NgZone): TelemetrySubscriber {
    const keys = this.apiKeys.map(key => ({type: EntityKeyType.TIME_SERIES, key}));
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.apiUsageState},
      pageLink: {page: 0, pageSize: 1},
      entityFields: [],
      latestValues: keys
    };
    cmd.latestCmd = {keys};
    const subscriber = new TelemetrySubscriber(telemetryWsService, zone);
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityData$.subscribe(update => {
      const entity = (update.data?.data ?? update.update ?? [])[0];
      if (!entity && !update.data) {
        return;
      }
      const values = {...(update.data ? {} : this.apiUsage() ?? {})};
      this.apiKeys.forEach(key => {
        const latest = entity?.latest?.[EntityKeyType.TIME_SERIES]?.[key];
        if (latest?.ts) {
          const number = Number(latest.value);
          values[key] = isFinite(number) ? number : 0;
        }
      });
      this.apiUsage.set(values);
      this.chooseTab();
    });
    subscriber.subscribe();
    return subscriber;
  }

  private chooseTab(): void {
    const groups = this.groups();
    if (this.tabChosen || !groups.length) {
      return;
    }
    this.tabChosen = true;
    const [entities, api] = groups;
    if (api.rows.some(row => row.critical) && !entities.rows.some(row => row.critical)) {
      this.tab.set('api');
    }
  }

  private toRow(name: string, used: number, limit: number): UsageRow {
    const percent = limit ? Math.min(100, (used ?? 0) / limit * 100) : 0;
    return {name, used: used ?? 0, limit: limit ?? 0, percent, critical: limit > 0 && percent >= CRITICAL_PERCENT};
  }
}
