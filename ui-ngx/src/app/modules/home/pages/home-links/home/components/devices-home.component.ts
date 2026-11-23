// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  NgZone,
  signal,
  untracked,
  WritableSignal
} from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser, selectPersistDeviceStateToTelemetry } from '@core/auth/auth.selectors';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityCountCmd, EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  BooleanOperation,
  EntityData,
  EntityDataPageLink,
  EntityKey,
  EntityKeyType,
  EntityKeyValueType,
  FilterPredicateType,
  KeyFilter,
  NumericOperation
} from '@shared/models/query/query.models';
import { Direction } from '@shared/models/page/sort-order';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';
import { HelpLinks } from '@shared/models/constants';
import { HomeCardId } from '@shared/models/user-settings.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';
import { homeAgo, homeListPageSize, homeListShown } from '@home/pages/home-links/home/home-list.models';

type DeviceState = 'inactive' | 'active';
type DeviceSortKey = 'last' | 'name';

const MINUTE = 60000;

interface DeviceQuery {
  state: DeviceState | null;
  sortKey: EntityKey;
  direction: Direction;
  withActivity?: boolean;
  neverConnected?: boolean;
  counted: boolean;
}

interface DeviceTile {
  state: DeviceState;
  count: number;
  pressed: boolean;
}

export interface DeviceHomeRow {
  id: string;
  name: string;
  profile: string;
  active: boolean;
  lastActivity: number | null;
  ago: string;
  url: string;
}

@Component({
  selector: 'tb-devices-home',
  templateUrl: './devices-home.component.html',
  styleUrls: ['./devices-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DevicesHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.DEVICES];

  readonly howToLink = HelpLinks.linksMap.devices;

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private store = inject(Store<AppState>);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private userPermissions = inject(UserPermissionsService);

  private readonly authUser = getCurrentAuthUser(this.store);
  readonly tenantAdmin = this.authUser?.authority === Authority.TENANT_ADMIN;
  readonly canAdd = this.tenantAdmin && this.userPermissions.hasGenericPermission(Resource.DEVICE, Operation.CREATE);

  readonly filter = signal<DeviceState | null>(null);
  readonly expanded = signal(false);
  readonly sortKey = signal<DeviceSortKey>('last');
  readonly sortDesc = signal(true);

  readonly total = signal<number | null>(null);
  readonly inactive = signal<number | null>(null);
  readonly active = signal<number | null>(null);

  private readonly stateKeyType = signal<EntityKeyType | null>(null);
  private readonly lists = signal<(EntityData[] | null)[]>([]);
  private readonly listTotals = signal<number[]>([]);
  private readonly now = signal(Date.now());

  private countSubscribers: TelemetrySubscriber[] = [];
  private dataSubscribers: TelemetrySubscriber[] = [];

  readonly tiles = computed<DeviceTile[]>(() => {
    const filter = this.filter();
    return [
      {state: 'inactive', count: this.inactive() ?? 0, pressed: filter === 'inactive'},
      {state: 'active', count: this.active() ?? 0, pressed: filter === 'active'}
    ];
  });

  readonly allActive = computed(() => !this.filter() && this.inactive() === 0 && !!this.total());

  readonly loaded = computed(() => this.lists().length > 0 && this.lists().every(list => list !== null));

  readonly listed = computed(() => this.listTotals().reduce((sum, count, index) =>
    this.listQueries[index]?.counted ? sum + count : sum, 0));

  private listQueries: DeviceQuery[] = [];

  readonly rows = computed<DeviceHomeRow[]>(() => {
    const keyType = this.stateKeyType();
    const now = this.now();
    const merged = this.lists().flatMap((list, index) => {
      const rows = list ?? [];
      return this.listQueries[index]?.neverConnected && keyType
        ? rows.filter(data => !Number(data.latest?.[keyType]?.lastActivityTime?.value))
        : rows;
    });
    return merged.slice(0, homeListShown(this.listed(), this.expanded())).map(data => this.toRow(data, keyType, now));
  });

  readonly viewAllParams = computed(() => {
    const filter = this.filter();
    return filter ? {active: filter === 'active'} : null;
  });

  private readonly queries = computed<DeviceQuery[]>(() => {
    const keyType = this.stateKeyType();
    if (!keyType) {
      return [];
    }
    const filter = this.filter();
    const desc = this.sortDesc();
    if (this.sortKey() === 'name') {
      return [{state: filter, sortKey: {type: EntityKeyType.ENTITY_FIELD, key: 'name'}, direction: desc ? Direction.DESC : Direction.ASC, counted: true}];
    }
    const lastActivity: EntityKey = {type: keyType, key: 'lastActivityTime'};
    const silent: DeviceQuery = {state: 'inactive', sortKey: lastActivity, direction: desc ? Direction.DESC : Direction.ASC,
      withActivity: true, counted: false};
    const never: DeviceQuery = {state: 'inactive', sortKey: lastActivity, direction: Direction.DESC, neverConnected: true, counted: true};
    const active: DeviceQuery = {state: 'active', sortKey: lastActivity, direction: desc ? Direction.ASC : Direction.DESC, counted: true};
    const inactive = desc ? [silent, never] : [never, silent];
    if (filter === 'inactive') {
      return inactive;
    }
    if (filter === 'active') {
      return [active];
    }
    return desc ? [...inactive, active] : [active, ...inactive];
  });

  constructor() {
    this.store.select(selectPersistDeviceStateToTelemetry).pipe(take(1)).subscribe(persisted => {
      const keyType = persisted ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
      this.countSubscribers = [
        this.subscribeCount(this.total),
        this.subscribeCount(this.inactive, this.activeFilter(keyType, false)),
        this.subscribeCount(this.active, this.activeFilter(keyType, true))
      ];
      this.stateKeyType.set(keyType);
    });
    effect(() => {
      const queries = this.queries();
      const pageSize = homeListPageSize(this.expanded());
      untracked(() => this.subscribeLists(queries, pageSize));
    });
    const tick = setInterval(() => this.now.set(Date.now()), MINUTE);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(tick);
      this.countSubscribers.forEach(subscriber => subscriber.unsubscribe());
      this.dataSubscribers.forEach(subscriber => subscriber.unsubscribe());
    });
  }

  pick(tile: DeviceTile): void {
    if (tile.count || tile.pressed) {
      this.filter.set(tile.pressed ? null : tile.state);
    }
  }

  sortBy(key: DeviceSortKey): void {
    if (this.sortKey() === key) {
      this.sortDesc.set(!this.sortDesc());
    } else {
      this.sortKey.set(key);
      this.sortDesc.set(key === 'last');
    }
  }

  ariaSort(key: DeviceSortKey): string {
    if (this.sortKey() !== key) {
      return 'none';
    }
    return this.sortDesc() ? 'descending' : 'ascending';
  }

  sortIcon(key: DeviceSortKey): string {
    if (key === 'name') {
      return this.sortKey() === 'name' && this.sortDesc() ? 'arrow_downward' : 'arrow_upward';
    }
    return this.sortKey() === 'last' && !this.sortDesc() ? 'arrow_upward' : 'arrow_downward';
  }

  open(row: DeviceHomeRow): void {
    this.router.navigateByUrl(row.url);
  }

  private subscribeLists(queries: DeviceQuery[], pageSize: number): void {
    this.dataSubscribers.forEach(subscriber => subscriber.unsubscribe());
    const keyType = this.stateKeyType();
    if (!keyType || !queries.length) {
      this.dataSubscribers = [];
      return;
    }
    this.listQueries = queries;
    const previous = this.lists();
    const previousTotals = this.listTotals();
    this.lists.set(queries.map((_, index) => previous[index] ?? null));
    this.listTotals.set(queries.map((_, index) => previousTotals[index] ?? 0));
    this.dataSubscribers = queries.map((query, index) => {
      const cmd = new EntityDataCmd();
      const pageLink: EntityDataPageLink = {
        page: 0,
        pageSize: query.neverConnected ? pageSize * 2 : pageSize,
        sortOrder: {key: query.sortKey, direction: query.direction},
        dynamic: true
      };
      cmd.query = {
        entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
        pageLink,
        keyFilters: [
          ...(query.state ? this.activeFilter(keyType, query.state === 'active') : []),
          ...(query.withActivity ? [this.hasActivityFilter(keyType)] : [])
        ],
        entityFields: [
          {type: EntityKeyType.ENTITY_FIELD, key: 'name'},
          {type: EntityKeyType.ENTITY_FIELD, key: 'type'}
        ],
        latestValues: [
          {type: keyType, key: 'active'},
          {type: keyType, key: 'lastActivityTime'}
        ]
      };
      cmd.latestCmd = {keys: [{type: keyType, key: 'active'}, {type: keyType, key: 'lastActivityTime'}]};
      const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
      subscriber.subscriptionCommands.push(cmd);
      subscriber.entityData$.subscribe(update => {
        if (update.data) {
          this.setList(index, update.data.data, update.data.totalElements);
        } else if (update.update) {
          const list = this.lists()[index] ?? [];
          const byId = new Map(update.update.map(data => [data.entityId.id, data]));
          this.setList(index, list.map(data => this.merge(data, byId.get(data.entityId.id))), null);
        }
      });
      subscriber.subscribe();
      return subscriber;
    });
  }

  private setList(index: number, data: EntityData[], total: number | null): void {
    const lists = [...this.lists()];
    lists[index] = data;
    this.lists.set(lists);
    if (total !== null) {
      const totals = [...this.listTotals()];
      totals[index] = total;
      this.listTotals.set(totals);
    }
  }

  private merge(current: EntityData, update?: EntityData): EntityData {
    if (!update) {
      return current;
    }
    const latest = {...current.latest};
    Object.keys(update.latest ?? {}).forEach(type => latest[type] = {...(latest[type] ?? {}), ...update.latest[type]});
    return {...current, latest};
  }

  private subscribeCount(target: WritableSignal<number | null>, keyFilters?: KeyFilter[]): TelemetrySubscriber {
    const cmd = new EntityCountCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      keyFilters
    };
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityCount$.subscribe(update => target.set(update.count ?? 0));
    subscriber.subscribe();
    return subscriber;
  }

  private activeFilter(keyType: EntityKeyType, active: boolean): KeyFilter[] {
    return [{
      key: {type: keyType, key: 'active'},
      valueType: EntityKeyValueType.BOOLEAN,
      predicate: {
        type: FilterPredicateType.BOOLEAN,
        operation: BooleanOperation.EQUAL,
        value: {defaultValue: active}
      }
    }];
  }

  private hasActivityFilter(keyType: EntityKeyType): KeyFilter {
    return {
      key: {type: keyType, key: 'lastActivityTime'},
      valueType: EntityKeyValueType.NUMERIC,
      predicate: {
        type: FilterPredicateType.NUMERIC,
        operation: NumericOperation.GREATER,
        value: {defaultValue: 0}
      }
    };
  }

  private toRow(data: EntityData, keyType: EntityKeyType | null, now: number): DeviceHomeRow {
    const fields = data.latest?.[EntityKeyType.ENTITY_FIELD] ?? {};
    const state = keyType ? data.latest?.[keyType] ?? {} : {};
    const lastActivity = Number(state.lastActivityTime?.value) || null;
    return {
      id: data.entityId.id,
      name: fields.name?.value ?? '',
      profile: fields.type?.value ?? '',
      active: state.active?.value === 'true',
      lastActivity,
      ago: lastActivity ? homeAgo(this.translate, now - lastActivity) : '',
      url: getEntityDetailsPageURL(data.entityId.id, EntityType.DEVICE)
    };
  }
}
