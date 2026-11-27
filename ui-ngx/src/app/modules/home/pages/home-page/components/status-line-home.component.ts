// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, NgZone, signal, WritableSignal } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser, selectPersistDeviceStateToTelemetry } from '@core/auth/auth.selectors';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AlarmCountCmd, EntityCountCmd, EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityKeyType, EntityKeyValueType, FilterPredicateType, BooleanOperation, KeyFilter } from '@shared/models/query/query.models';
import { AlarmSearchStatus, AlarmSeverity } from '@shared/models/alarm.models';
import { Authority } from '@shared/models/authority.enum';
import { HomePermissionsService } from '@home/pages/home-page/home-permissions.service';
import { HomeCardId } from '@shared/models/user-settings.models';
import { AdminService } from '@core/http/admin.service';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';
import { TranslateService } from '@ngx-translate/core';
import { FeaturesInfo } from '@shared/models/settings.models';
import { SubscriptionInfo } from '@shared/models/subscription.models';
import { of } from 'rxjs';
import { catchError, take } from 'rxjs/operators';

type StatusKind = 'crit' | 'warn' | 'ok';

interface StatusPart {
  key: string;
  params?: Record<string, unknown>;
  cards?: HomeCardId[];
  link?: string;
}

type SystemMetric = 'cpu' | 'ram' | 'disk';

interface SystemLoad {
  metric: SystemMetric;
  value: number;
  services: string[];
}

interface ClusterService {
  serviceId: string;
  cpuUsage: number;
  memoryUsage: number;
  discUsage: number;
}

const SYSTEM_KEYS = ['cpuUsage', 'memoryUsage', 'discUsage', 'clusterMode', 'clusterSystemData'];
const METRIC_KEYS: Record<SystemMetric, 'cpuUsage' | 'memoryUsage' | 'discUsage'> = {cpu: 'cpuUsage', ram: 'memoryUsage', disk: 'discUsage'};
const METRIC_NAMES: Record<SystemMetric, string> = {cpu: 'CPU', ram: 'RAM', disk: 'Disk'};
const LICENSE_NEAR = 0.85;
const WARN_LOAD = 75;
const CRIT_LOAD = 85;

const loadLevel = (value: number): StatusKind => value > CRIT_LOAD ? 'crit' : value > WARN_LOAD ? 'warn' : 'ok';

@Component({
  selector: 'tb-status-line-home',
  template: `
    @if (parts().length) {
      <div class="tb-status-line tb-status-line-in" [class]="kind()">
        @for (part of parts(); track part.key; let first = $first) {
          <span class="tb-pill">
            @if (first) {
              @if (kind() === 'crit') {
                <tb-ai-sev-mark severity="CRITICAL"></tb-ai-sev-mark>
              } @else {
                <span class="tb-status-dot"></span>
              }
            }
            @if (part.link || part.cards) {
              <a class="tb-status-link" href="#" (click)="$event.preventDefault(); open(part)">{{ part.key | translate: part.params }}</a>
            } @else {
              <span>{{ part.key | translate: part.params }}</span>
            }
          </span>
        }
      </div>
    } @else if (reserve && !resolved()) {
      <div class="tb-status-line tb-status-line-reserved" aria-hidden="true"></div>
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

  private translate = inject(TranslateService);

  private readonly authority = getCurrentAuthUser(this.store)?.authority;
  private readonly customer = this.authority === Authority.CUSTOMER_USER;
  readonly sysAdmin = this.authority === Authority.SYS_ADMIN;
  private readonly homePermissions = inject(HomePermissionsService);
  private readonly canReadAlarms = !this.sysAdmin && this.homePermissions.canReadAlarms();
  private readonly canReadDevices = !this.sysAdmin && this.homePermissions.canReadEntities(EntityType.DEVICE);
  private readonly devicesLink = this.canReadDevices ? this.homePermissions.entitiesLink(EntityType.DEVICE) : null;

  private readonly shownKey = `tb-home-status-line:${getCurrentAuthUser(this.store)?.userId}`;

  readonly reserve = this.authority !== Authority.TENANT_ADMIN || this.wasShown();

  private systemLoad = signal<SystemLoad | null>(null);
  private subscription = signal<SubscriptionInfo | null | undefined>(undefined);
  private features = signal<FeaturesInfo | null | undefined>(undefined);
  private tenants = signal<number | null>(null);

  private critical = signal<number | null>(null);
  private active = signal<number | null>(null);
  private devices = signal<number | null>(null);
  private inactive = signal<number | null>(null);

  private subscribers: TelemetrySubscriber[] = [];

  readonly parts = computed<StatusPart[]>(() => this.sysAdmin ? this.sysAdminParts() : this.tenantParts());

  readonly resolved = computed(() => this.sysAdmin
    ? !!this.systemLoad() && this.subscription() !== undefined && this.tenants() !== null
    : [this.critical(), this.active(), this.devices(), this.inactive()].every(value => value !== null));

  readonly kind = computed<StatusKind>(() => this.sysAdmin ? this.sysAdminKind() : this.critical() ? 'crit' : this.inactive() ? 'warn' : 'ok');

  private tenantParts(): StatusPart[] {
    const critical = this.canReadAlarms ? this.critical() : 0;
    const active = this.canReadAlarms ? this.active() : 0;
    const devices = this.canReadDevices ? this.devices() : 0;
    const inactive = this.canReadDevices ? this.inactive() : 0;
    if (critical === null || active === null || devices === null || inactive === null) {
      return [];
    }
    if (!devices) {
      return this.customer && this.canReadDevices ? [{key: 'home.status.nothing-assigned'}] : [];
    }
    const parts: StatusPart[] = [];
    if (this.canReadAlarms) {
      if (critical) {
        parts.push({key: 'home.status.critical-alarms', params: {count: critical}, cards: [HomeCardId.ALARMS], link: '/alarms'});
      } else if (active) {
        parts.push({key: 'home.status.active-alarms', params: {count: active}, cards: [HomeCardId.ALARMS], link: '/alarms'});
      } else {
        parts.push({key: 'home.status.no-alarms'});
      }
    }
    if (inactive) {
      parts.push({key: 'home.status.inactive-devices', params: {count: inactive}, cards: [HomeCardId.DEVICES], link: this.devicesLink});
    } else {
      parts.push({key: devices === 1 ? 'home.status.device-active' : 'home.status.all-devices-active', params: {count: devices}});
    }
    return parts;
  }

  private sysAdminParts(): StatusPart[] {
    const load = this.systemLoad();
    const subscription = this.subscription();
    const features = this.features();
    const tenants = this.tenants();
    if (!load || subscription === undefined || tenants === null) {
      return [];
    }
    const parts: StatusPart[] = [];
    if (loadLevel(load.value) === 'ok') {
      parts.push({key: 'home.status.system-ok'});
    } else {
      const params = {name: METRIC_NAMES[load.metric], value: Math.round(load.value)};
      if (!load.services.length) {
        parts.push({key: 'home.status.system-high', params, cards: [HomeCardId.SYSTEM_STATUS]});
      } else if (load.services.length === 1) {
        parts.push({key: 'home.status.system-high-on', params: {...params, service: load.services[0]}, cards: [HomeCardId.SYSTEM_STATUS]});
      } else {
        parts.push({key: 'home.status.system-high-services', params: {...params, count: load.services.length}, cards: [HomeCardId.SYSTEM_STATUS]});
      }
    }
    const near = this.licenseNear(subscription);
    if (near.length) {
      parts.push({key: 'home.status.license-near', params: {items: near.join(', ')}, cards: [HomeCardId.LICENSE], link: '/license'});
    }
    if (!tenants) {
      parts.push({key: 'home.status.no-tenants', cards: [HomeCardId.GET_STARTED_SYS_ADMIN], link: '/tenants'});
    } else if (features) {
      const off = [features.whiteLabelingEnabled, features.emailEnabled,
        features.smsEnabled, features.notificationEnabled, features.oauthEnabled, features.twoFaEnabled].filter(enabled => !enabled).length;
      if (off) {
        parts.push({key: 'home.status.features-off', params: {count: off},
          cards: [HomeCardId.GET_STARTED_SYS_ADMIN, HomeCardId.INSTALL_ITEMS]});
      }
    }
    return parts;
  }

  private sysAdminKind(): StatusKind {
    const load = this.systemLoad();
    const level = load ? loadLevel(load.value) : 'ok';
    if (level === 'crit') {
      return 'crit';
    }
    const subscription = this.subscription();
    return level === 'warn' || (subscription && this.licenseNear(subscription).length) ? 'warn' : 'ok';
  }

  private licenseNear(subscription: SubscriptionInfo | null): string[] {
    if (!subscription) {
      return [];
    }
    const items: [string, number, number][] = [
      ['home.status.license-devices', subscription.devicesCount, subscription.maxDevices],
      ['home.status.license-assets', subscription.assetsCount, subscription.maxAssets],
      ['home.status.license-ai-credits', subscription.usedAiCredits, subscription.maxAiCredits]
    ];
    return items.filter(([, used, max]) => max > 0 && (used ?? 0) / max >= LICENSE_NEAR)
      .map(([key, used, max]) => this.translate.instant(key, {percent: Math.round(used / max * 100)}));
  }

  constructor() {
    inject(DestroyRef).onDestroy(() => this.subscribers.forEach(subscriber => subscriber.unsubscribe()));
    effect(() => {
      if (this.resolved()) {
        this.rememberShown(this.parts().length > 0);
      }
    });
    if (this.sysAdmin) {
      this.watchSysAdmin();
    } else {
      this.watchTenant();
    }
  }

  private watchTenant(): void {
    this.store.select(selectPersistDeviceStateToTelemetry).pipe(take(1)).subscribe(persisted => {
      const activeKeyType = persisted ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
      if (this.canReadAlarms) {
        this.countAlarms(this.critical, [AlarmSeverity.CRITICAL]);
        this.countAlarms(this.active);
      }
      if (!this.canReadDevices) {
        return;
      }
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
  }

  private wasShown(): boolean {
    try {
      return localStorage.getItem(this.shownKey) === '1';
    } catch {
      return false;
    }
  }

  private rememberShown(shown: boolean): void {
    try {
      localStorage.setItem(this.shownKey, shown ? '1' : '0');
    } catch {
      return;
    }
  }

  open(part: StatusPart): void {
    const card = (part.cards ?? []).map(id => document.querySelector(`[data-home-card="${id}"]`)).find(element => !!element);
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

  private watchSysAdmin(): void {
    const adminService = inject(AdminService);
    const config = {ignoreErrors: true, ignoreLoading: true};
    adminService.getSubscriptionInfo(config).pipe(catchError(() => of(null))).subscribe(info => this.subscription.set(info));
    const sysAdminHome = inject(SysAdminHomeService, {optional: true});
    (sysAdminHome ? sysAdminHome.featuresInfo$ : adminService.getFeaturesInfo(config).pipe(catchError(() => of(null))))
      .subscribe(info => this.features.set(info));
    this.countEntities(this.tenants, EntityType.TENANT);
    const keys = SYSTEM_KEYS.map(key => ({type: EntityKeyType.TIME_SERIES, key}));
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.apiUsageState},
      pageLink: {page: 0, pageSize: 1},
      entityFields: [],
      latestValues: keys
    };
    cmd.latestCmd = {keys};
    const values: Record<string, string> = {};
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    this.subscribers.push(subscriber);
    subscriber.entityData$.subscribe(update => {
      const entity = (update.data?.data ?? update.update ?? [])[0];
      SYSTEM_KEYS.forEach(key => {
        const latest = entity?.latest?.[EntityKeyType.TIME_SERIES]?.[key];
        if (latest?.ts) {
          values[key] = latest.value;
        }
      });
      if (entity || update.data) {
        this.systemLoad.set(this.toSystemLoad(values));
      }
    });
    subscriber.subscribe();
  }

  private toSystemLoad(values: Record<string, string>): SystemLoad {
    const metrics = Object.keys(METRIC_KEYS) as SystemMetric[];
    if (values.clusterMode === 'true' && values.clusterSystemData) {
      let services: ClusterService[] = [];
      try {
        services = JSON.parse(values.clusterSystemData);
      } catch {
        services = [];
      }
      if (services.length) {
        const loads = metrics.flatMap(metric => services.map(service =>
          ({metric, value: Number(service[METRIC_KEYS[metric]]) || 0, service: service.serviceId})));
        const worst = loads.reduce((top, load) => load.value > top.value ? load : top);
        return {
          metric: worst.metric,
          value: worst.value,
          services: loads.filter(load => load.metric === worst.metric && load.value === worst.value).map(load => load.service)
        };
      }
    }
    return metrics.map(metric => ({metric, value: Number(values[METRIC_KEYS[metric]]) || 0, services: []}))
      .reduce((top, load) => load.value > top.value ? load : top);
  }

  private countDevices(target: WritableSignal<number | null>, keyFilters?: KeyFilter[]): void {
    this.countEntities(target, EntityType.DEVICE, keyFilters);
  }

  private countEntities(target: WritableSignal<number | null>, entityType: EntityType, keyFilters?: KeyFilter[]): void {
    const cmd = new EntityCountCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType},
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
