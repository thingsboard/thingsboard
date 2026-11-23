// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, NgZone, signal, WritableSignal } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser, selectPersistDeviceStateToTelemetry } from '@core/auth/auth.selectors';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AlarmCountCmd, EntityCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityKeyType, EntityKeyValueType, FilterPredicateType, BooleanOperation, KeyFilter } from '@shared/models/query/query.models';
import { AlarmSearchStatus, AlarmSeverity } from '@shared/models/alarm.models';
import { Authority } from '@shared/models/authority.enum';
import { HomeCardId } from '@shared/models/user-settings.models';
import { take } from 'rxjs/operators';

type StatusKind = 'crit' | 'warn' | 'ok';

interface StatusPart {
  key: string;
  params?: Record<string, unknown>;
  card?: HomeCardId;
  link?: string;
}

@Component({
  selector: 'tb-status-line-home',
  template: `
    @if (parts().length) {
      <div class="tb-status-line" [class]="kind()">
        @for (part of parts(); track part.key; let first = $first) {
          <span class="tb-pill">
            @if (first) {
              @if (kind() === 'crit') {
                <tb-ai-sev-mark severity="CRITICAL"></tb-ai-sev-mark>
              } @else {
                <span class="tb-status-dot"></span>
              }
            }
            @if (part.link) {
              <a class="tb-status-link" href="#" (click)="$event.preventDefault(); open(part)">{{ part.key | translate: part.params }}</a>
            } @else {
              <span>{{ part.key | translate: part.params }}</span>
            }
          </span>
        }
      </div>
    }
  `,
  styleUrls: ['./status-line-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusLineHomeComponent {

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private router = inject(Router);
  private store = inject(Store<AppState>);

  private readonly customer = getCurrentAuthUser(this.store)?.authority === Authority.CUSTOMER_USER;

  private critical = signal<number | null>(null);
  private active = signal<number | null>(null);
  private devices = signal<number | null>(null);
  private inactive = signal<number | null>(null);

  private subscribers: TelemetrySubscriber[] = [];

  readonly parts = computed<StatusPart[]>(() => {
    const critical = this.critical();
    const active = this.active();
    const devices = this.devices();
    const inactive = this.inactive();
    if (critical === null || active === null || devices === null || inactive === null) {
      return [];
    }
    if (!devices) {
      return this.customer ? [{key: 'home.status.nothing-assigned'}] : [];
    }
    const parts: StatusPart[] = [];
    if (critical) {
      parts.push({key: 'home.status.critical-alarms', params: {count: critical}, card: HomeCardId.ALARMS, link: '/alarms'});
    } else if (active) {
      parts.push({key: 'home.status.active-alarms', params: {count: active}, card: HomeCardId.ALARMS, link: '/alarms'});
    } else {
      parts.push({key: 'home.status.no-alarms'});
    }
    if (inactive) {
      parts.push({key: 'home.status.inactive-devices', params: {count: inactive}, card: HomeCardId.DEVICES, link: '/entities/devices'});
    } else {
      parts.push({key: devices === 1 ? 'home.status.device-active' : 'home.status.all-devices-active', params: {count: devices}});
    }
    return parts;
  });

  readonly kind = computed<StatusKind>(() => this.critical() ? 'crit' : this.inactive() ? 'warn' : 'ok');

  constructor() {
    this.store.select(selectPersistDeviceStateToTelemetry).pipe(take(1)).subscribe(persisted => {
      const activeKeyType = persisted ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
      this.countAlarms(this.critical, [AlarmSeverity.CRITICAL]);
      this.countAlarms(this.active);
      this.countDevices(this.devices);
      this.countDevices(this.inactive, [{
        key: {type: activeKeyType, key: 'active'},
        valueType: EntityKeyValueType.BOOLEAN,
        predicate: {
          type: FilterPredicateType.BOOLEAN,
          operation: BooleanOperation.EQUAL,
          value: {defaultValue: false}
        }
      }]);
    });
    inject(DestroyRef).onDestroy(() => this.subscribers.forEach(subscriber => subscriber.unsubscribe()));
  }

  open(part: StatusPart): void {
    const card = part.card ? document.querySelector(`[data-home-card="${part.card}"]`) : null;
    if (card) {
      card.scrollIntoView({behavior: 'smooth', block: 'center'});
    } else if (part.link) {
      this.router.navigateByUrl(part.link);
    }
  }

  private countAlarms(target: WritableSignal<number | null>, severityList?: AlarmSeverity[]): void {
    const cmd = new AlarmCountCmd();
    cmd.query = {
      entityFilter: null,
      statusList: [AlarmSearchStatus.ACTIVE],
      ...(severityList ? {severityList} : {})
    };
    const subscriber = this.subscribe(cmd);
    subscriber.alarmCount$.subscribe(update => target.set(update.count ?? 0));
    subscriber.subscribe();
  }

  private countDevices(target: WritableSignal<number | null>, keyFilters?: KeyFilter[]): void {
    const cmd = new EntityCountCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      keyFilters
    };
    const subscriber = this.subscribe(cmd);
    subscriber.entityCount$.subscribe(update => target.set(update.count ?? 0));
    subscriber.subscribe();
  }

  private subscribe(cmd: AlarmCountCmd | EntityCountCmd): TelemetrySubscriber {
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    this.subscribers.push(subscriber);
    return subscriber;
  }
}
