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
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AlarmService } from '@core/http/alarm.service';
import { DialogService } from '@core/services/dialog.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { AlarmCountCmd, AlarmDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AlarmData, AlarmDataPageLink, EntityKeyType } from '@shared/models/query/query.models';
import { Direction } from '@shared/models/page/sort-order';
import { AlarmSearchStatus, AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';
import { AlarmDetailsDialogComponent, AlarmDetailsDialogData } from '@home/components/alarm/alarm-details-dialog.component';
import { homeAgo, homeListPageSize, homeListShown } from '@home/pages/home-links/home/home-list.models';
import { HomeToggleOption } from '@home/pages/home-links/home/components/home-toggle.component';

const MINUTE = 60000;
const MINE_KEY = 'tb-home-alarms-mine';
const SEVERITIES = [AlarmSeverity.CRITICAL, AlarmSeverity.MAJOR, AlarmSeverity.MINOR, AlarmSeverity.WARNING];

type AlarmSortKey = 'severity' | 'createdTime';

interface SeverityTile {
  severity: AlarmSeverity;
  count: number;
  pressed: boolean;
  hot: boolean;
}

export interface AlarmHomeRow {
  alarm: AlarmData;
  ago: string;
  originatorLink: string | null;
  assigneeName: string | null;
  initials: string;
  mine: boolean;
}

const readMine = (): boolean => {
  try {
    return localStorage.getItem(MINE_KEY) === '1';
  } catch {
    return false;
  }
};

const writeMine = (mine: boolean): void => {
  try {
    if (mine) {
      localStorage.setItem(MINE_KEY, '1');
    } else {
      localStorage.removeItem(MINE_KEY);
    }
  } catch {
    return;
  }
};

@Component({
  selector: 'tb-alarms-home',
  templateUrl: './alarms-home.component.html',
  styleUrls: ['./alarms-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AlarmsHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.ALARMS];

  readonly whoseOptions: HomeToggleOption<boolean>[] = [
    {value: false, label: 'home.alarms.all'},
    {value: true, label: 'home.alarms.mine'}
  ];

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private store = inject(Store<AppState>);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private dialogService = inject(DialogService);
  private alarmService = inject(AlarmService);
  private toast = inject(ToastNotificationService);
  private translate = inject(TranslateService);

  private readonly authUser = getCurrentAuthUser(this.store);
  private readonly userId = this.authUser?.userId;
  readonly tenantAdmin = this.authUser?.authority === Authority.TENANT_ADMIN;

  readonly mine = signal(readMine());
  readonly severity = signal<AlarmSeverity | null>(null);
  readonly expanded = signal(false);
  readonly sortKey = signal<AlarmSortKey>('severity');
  readonly sortDesc = signal(true);

  readonly total = signal<number | null>(null);
  private readonly severityCounts = new Map<AlarmSeverity, WritableSignal<number | null>>(
    SEVERITIES.map(severity => [severity, signal<number | null>(null)])
  );

  private readonly alarms = signal<AlarmData[] | null>(null);
  readonly listed = signal(0);
  readonly alarmsLoaded = computed(() => this.alarms() !== null);
  private readonly now = signal(Date.now());

  private totalSubscriber: TelemetrySubscriber | null = null;
  private severitySubscribers: TelemetrySubscriber[] = [];
  private dataSubscriber: TelemetrySubscriber | null = null;

  readonly tiles = computed<SeverityTile[]>(() => {
    const pressed = this.severity();
    return SEVERITIES.map(severity => {
      const count = this.severityCounts.get(severity)() ?? 0;
      return {severity, count, pressed: pressed === severity, hot: severity === AlarmSeverity.CRITICAL && count > 0};
    }).filter(tile => tile.severity === AlarmSeverity.CRITICAL || tile.count > 0 || tile.pressed);
  });

  readonly rows = computed<AlarmHomeRow[]>(() => {
    const now = this.now();
    return (this.alarms() ?? []).slice(0, homeListShown(this.listed(), this.expanded())).map(alarm => this.toRow(alarm, now));
  });

  readonly emptyNote = computed<{key: string; params: Record<string, string>}>(() => {
    const severity = this.severity();
    const mine = this.mine();
    if (severity) {
      return {
        key: mine ? 'home.alarms.none-severity-mine' : 'home.alarms.none-severity',
        params: {severity: this.translate.instant(alarmSeverityTranslations.get(severity)).toLowerCase()}
      };
    }
    return {key: mine ? 'home.alarms.none-mine' : 'home.alarms.none-active', params: {}};
  });

  readonly viewAllParams = computed<Record<string, unknown>>(() => {
    const severity = this.severity();
    return {
      ...(severity ? {severityList: [severity]} : {}),
      ...(this.mine() ? {assignedToMe: true} : {})
    };
  });

  private readonly pageLink = computed<AlarmDataPageLink>(() => {
    const severity = this.severity();
    const key = this.sortKey();
    const desc = this.sortDesc();
    return {
      page: 0,
      pageSize: homeListPageSize(this.expanded()),
      sortOrder: {
        key: {type: EntityKeyType.ALARM_FIELD, key},
        direction: key === 'severity' ? (desc ? Direction.ASC : Direction.DESC) : (desc ? Direction.DESC : Direction.ASC)
      },
      statusList: [AlarmSearchStatus.ACTIVE],
      ...(severity ? {severityList: [severity]} : {}),
      ...(this.mine() && this.userId ? {assigneeId: {entityType: EntityType.USER, id: this.userId}} : {})
    };
  });

  constructor() {
    this.totalSubscriber = this.subscribeCount(this.total);
    effect(() => {
      const mine = this.mine();
      untracked(() => this.subscribeSeverityCounts(mine));
    });
    effect(() => {
      const pageLink = this.pageLink();
      untracked(() => this.subscribeAlarms(pageLink));
    });
    const tick = setInterval(() => this.now.set(Date.now()), MINUTE);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(tick);
      this.totalSubscriber?.unsubscribe();
      this.severitySubscribers.forEach(subscriber => subscriber.unsubscribe());
      this.dataSubscriber?.unsubscribe();
    });
  }

  severityLabel(severity: AlarmSeverity | string): string {
    return alarmSeverityTranslations.get(severity as AlarmSeverity) ?? String(severity);
  }

  setMine(mine: boolean): void {
    if (mine !== this.mine()) {
      this.mine.set(mine);
      writeMine(mine);
    }
  }

  pick(tile: SeverityTile): void {
    if (tile.count || tile.pressed) {
      this.severity.set(tile.pressed ? null : tile.severity);
    }
  }

  sortBy(key: AlarmSortKey): void {
    if (this.sortKey() === key) {
      this.sortDesc.set(!this.sortDesc());
    } else {
      this.sortKey.set(key);
      this.sortDesc.set(true);
    }
  }

  ariaSort(key: AlarmSortKey): string {
    if (this.sortKey() !== key) {
      return 'none';
    }
    return this.sortDesc() ? 'descending' : 'ascending';
  }

  addAlarmRule(): void {
    this.router.navigate(['/alarms/alarm-rules'], {queryParams: {action: 'add'}});
  }

  openDetails(row: AlarmHomeRow, $event?: Event): void {
    $event?.preventDefault();
    $event?.stopPropagation();
    this.dialog.open<AlarmDetailsDialogComponent, AlarmDetailsDialogData, boolean>(AlarmDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        alarmId: row.alarm.id.id,
        alarm: row.alarm,
        allowAcknowledgment: true,
        allowClear: true,
        displayDetails: true,
        allowAssign: true
      }
    }).afterClosed().subscribe(changed => {
      if (changed) {
        this.refresh();
      }
    });
  }

  stop($event: Event): void {
    $event.stopPropagation();
  }

  acknowledge(row: AlarmHomeRow, $event?: Event): void {
    $event?.stopPropagation();
    if (row.alarm.acknowledged) {
      return;
    }
    this.patch(row.alarm.id.id, {acknowledged: true});
    this.alarmService.ackAlarm(row.alarm.id.id).subscribe({
      next: () => {
        this.notify('home.alarms.acknowledged', row);
        this.refresh();
      },
      error: () => this.refresh()
    });
  }

  assignToMe(row: AlarmHomeRow, $event?: Event): void {
    $event?.stopPropagation();
    if (this.userId) {
      this.alarmService.assignAlarm(row.alarm.id.id, this.userId).subscribe(() => {
        this.notify('home.alarms.assigned', row);
        this.refresh();
      });
    }
  }

  clear(row: AlarmHomeRow): void {
    const name = this.alarmName(row);
    this.dialogService.confirm(
      this.translate.instant('home.alarms.clear-title', {name}),
      this.translate.instant('home.alarms.clear-text'),
      this.translate.instant('action.cancel'),
      this.translate.instant('home.alarms.clear')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.alarmService.clearAlarm(row.alarm.id.id).subscribe(() => {
          this.notify('home.alarms.cleared', row);
          this.refresh();
        });
      }
    });
  }

  private subscribeAlarms(pageLink: AlarmDataPageLink): void {
    this.dataSubscriber?.unsubscribe();
    const cmd = new AlarmDataCmd();
    cmd.query = {
      entityFilter: null,
      pageLink,
      alarmFields: [],
      entityFields: [],
      latestValues: []
    };
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    this.dataSubscriber = subscriber;
    subscriber.alarmData$.subscribe(update => {
      if (update.data) {
        this.listed.set(update.data.totalElements);
        this.alarms.set(update.data.data);
      } else if (update.update) {
        const byId = new Map(update.update.map(alarm => [alarm.id.id, alarm]));
        this.alarms.set((this.alarms() ?? []).map(alarm => byId.get(alarm.id.id) ?? alarm));
      }
    });
    subscriber.subscribe();
  }

  private subscribeSeverityCounts(mine: boolean): void {
    this.severitySubscribers.forEach(subscriber => subscriber.unsubscribe());
    this.severitySubscribers = SEVERITIES.map(severity => this.subscribeCount(this.severityCounts.get(severity), severity, mine));
  }

  private subscribeCount(target: WritableSignal<number | null>, severity?: AlarmSeverity, mine = false): TelemetrySubscriber {
    const cmd = new AlarmCountCmd();
    cmd.query = {
      entityFilter: null,
      statusList: [AlarmSearchStatus.ACTIVE],
      ...(severity ? {severityList: [severity]} : {}),
      ...(mine && this.userId ? {assigneeId: {entityType: EntityType.USER, id: this.userId}} : {})
    };
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    subscriber.alarmCount$.subscribe(update => target.set(update.count ?? 0));
    subscriber.subscribe();
    return subscriber;
  }

  private refresh(): void {
    this.subscribeAlarms(this.pageLink());
    this.totalSubscriber?.unsubscribe();
    this.totalSubscriber = this.subscribeCount(this.total);
    this.subscribeSeverityCounts(this.mine());
  }

  private patch(alarmId: string, change: Partial<AlarmData>): void {
    const alarms = this.alarms();
    if (alarms) {
      this.alarms.set(alarms.map(alarm => alarm.id.id === alarmId ? {...alarm, ...change} : alarm));
    }
  }

  private notify(key: string, row: AlarmHomeRow): void {
    this.toast.dispatchNotification({
      message: this.translate.instant(key, {name: this.alarmName(row)}),
      type: 'info',
      duration: 4000,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast'
    });
  }

  private alarmName(row: AlarmHomeRow): string {
    return this.translate.instant('home.alarms.name', {type: row.alarm.type, originator: row.alarm.originatorName ?? ''});
  }

  private toRow(alarm: AlarmData, now: number): AlarmHomeRow {
    const assignee = alarm.assigneeId ? alarm.assignee : null;
    const assigneeName = assignee ? [assignee.firstName, assignee.lastName].filter(Boolean).join(' ') || assignee.email : null;
    const initials = assignee
      ? (assignee.firstName && assignee.lastName ? assignee.firstName[0] + assignee.lastName[0] : (assigneeName ?? '').slice(0, 2)).toUpperCase()
      : '';
    const originator = alarm.originator;
    return {
      alarm,
      ago: homeAgo(this.translate, now - (alarm.createdTime ?? now)),
      originatorLink: originator ? getEntityDetailsPageURL(originator.id, originator.entityType as EntityType) : null,
      assigneeName,
      initials,
      mine: !!alarm.assigneeId && alarm.assigneeId.id === this.userId
    };
  }
}
