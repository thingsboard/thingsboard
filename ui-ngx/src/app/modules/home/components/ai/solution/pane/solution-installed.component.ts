// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, DestroyRef, ElementRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { EntityResult, SolutionCreatorInfo, SolutionInstallProgress, SolutionProgressItem } from '@shared/models/solution-creator.models';
import {
  baseDetailsPageByEntityType,
  EntityType,
  entityTypeTranslations,
  groupUrlPrefixByEntityType
} from '@shared/models/entity-type.models';
import { AlarmSeverity, alarmSeverityColors, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import {
  AggregatedEntityResult,
  aggregateByName,
  IssueStatus
} from '@home/components/ai/solution/solution-info/issues-aggregation';

interface InstalledSection {
  id: string;
  title: string;
  link: string | null;
  rows: InstalledRow[];
}

interface RowChip {
  label: string;
  icon: string;
  palette: string;
}

interface RowDetails {
  role?: string;
  where?: string;
  chip?: RowChip;
  kindLabel?: string;
  ops?: string[];
  severities?: AlarmSeverity[];
}

type InstalledRow = AggregatedEntityResult & {
  type: EntityType;
  icon: string;
  palette: string;
  details: RowDetails;
  progress?: 'wait' | 'now';
};

interface SectionSpec {
  id: string;
  types: EntityType[];
  title: string;
  link: string | null;
}

const GROUPS: SectionSpec[] = [
  {id: 'dashboards', types: [EntityType.DASHBOARD], title: 'entity.type-dashboards', link: '/dashboards/all'},
  {id: 'devices', types: [EntityType.DEVICE], title: 'entity.type-devices', link: '/entities/devices/all'},
  {id: 'assets', types: [EntityType.ASSET], title: 'entity.type-assets', link: '/entities/assets/all'},
  {id: 'customers', types: [EntityType.CUSTOMER], title: 'entity.type-customers', link: '/customers/all'},
  {id: 'users', types: [EntityType.USER], title: 'entity.type-users', link: '/users/all'},
  {id: 'profiles', types: [EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE], title: 'solution-creator.pane.profiles', link: '/profiles/deviceProfiles'},
  {id: 'roles', types: [EntityType.ROLE], title: 'entity.type-roles', link: '/security-settings/roles'},
  {id: 'groups', types: [EntityType.ENTITY_GROUP], title: 'entity.type-entity-groups', link: null},
  {id: 'cfs', types: [EntityType.CALCULATED_FIELD], title: 'entity.type-calculated-fields', link: '/calculatedFields'},
  {id: 'alarms', types: [EntityType.ALARM], title: 'solution-creator.pane.alarm-rules', link: '/alarms/alarm-rules'},
  {id: 'chains', types: [EntityType.RULE_CHAIN], title: 'entity.type-rulechains', link: null}
];

const OPERATIONS: Record<string, string> = {C: 'create', R: 'read', U: 'update', D: 'delete'};

const SECTIONS: EntityType[] = GROUPS.flatMap(group => group.types);

const SUPPORTING: EntityType[] = [EntityType.ENTITY_GROUP, EntityType.RULE_CHAIN];

const GENERATED: EntityType[] = [EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.USER];

const STEP_MS = 160;

@Component({
  selector: 'tb-solution-installed',
  templateUrl: './solution-installed.component.html',
  styleUrls: ['./solution-pane.scss', './solution-installed.component.scss'],
  standalone: false,
  host: {
    '[class.narrow]': 'narrow()'
  }
})
export class SolutionInstalledComponent {

  solution = input.required<SolutionCreatorInfo>();
  creating = input(false);
  items = input<SolutionProgressItem[] | null>(null);
  stage = input<SolutionInstallProgress['stage'] | null>(null);
  createShare = input(0.85);
  seconds = input(90);
  startedAt = input(0);

  @Output() openMain = new EventEmitter<void>();

  private translate = inject(TranslateService);
  private authService = inject(AuthService);

  bannerClosed = signal(false);

  narrow = signal(false);

  private shown = signal(-1);
  private elapsed = signal(0);

  private planned = computed(() => {
    const items = this.items() ?? [];
    return SECTIONS.flatMap(type => items.filter(item => item.type === type));
  });

  private target = computed(() => {
    const planned = this.planned();
    if (!planned.length) {
      return 0;
    }
    const share = this.createShare();
    const install = this.stage() === 'INSTALL';
    const ready = planned.filter(item => install ? item.state === 'DONE' : item.state !== 'WORKING').length / planned.length;
    let count = Math.floor(planned.length * (install ? share + (1 - share) * ready : share * ready));
    const pending = planned.findIndex(item => !item.name);
    if (!install && pending >= 0) {
      count = Math.min(count, pending);
    } else if (!install && !planned.some(item => GENERATED.includes(item.type))) {
      count = Math.min(count, Math.max(0, planned.filter(item => item.type === EntityType.DASHBOARD).length - 1));
    }
    const paced = Math.floor(planned.length * this.elapsed() / Math.max(1, this.seconds()));
    return Math.min(count, paced, planned.length - 1);
  });

  constructor() {
    const created = Date.now();
    const timer = setInterval(() => {
      this.elapsed.set((Date.now() - (this.startedAt() || created)) / 1000);
      const target = this.target();
      if (this.shown() < 0) {
        this.shown.set(target);
      } else if (this.shown() < target) {
        this.shown.update(count => count + 1);
      }
    }, STEP_MS);
    const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    const resize = new ResizeObserver(() => this.narrow.set(host.clientWidth <= 560));
    resize.observe(host);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(timer);
      resize.disconnect();
    });
  }

  installed = computed(() => !!this.solution()?.metadata?.installed);

  sections = computed<InstalledSection[]>(() => {
    const results = this.solution()?.metadata?.installResult?.entityResults;
    const items = this.items();
    if (items?.length) {
      return this.plannedSections(items, results ?? {});
    }
    if (!results) {
      return [];
    }
    return this.grouped(type => aggregateByName(type, results[type])
      .filter(row => !SUPPORTING.includes(type) || row.status !== IssueStatus.OK)
      .map(row => this.row(type, row)));
  });

  entityCount = computed(() => this.sections()
    .filter(section => section.id !== 'dashboards')
    .reduce((count, section) => count + section.rows.length, 0));

  dashboardCount = computed(() => this.sections().find(section => section.id === 'dashboards')?.rows.length ?? 0);

  hasMain = computed(() => !!this.solution()?.metadata?.installResult?.mainDashboardId);

  private plannedSections(items: SolutionProgressItem[], results: Partial<Record<EntityType, EntityResult[]>>): InstalledSection[] {
    const shown = Math.max(0, this.shown());
    const install = this.stage() === 'INSTALL';
    let index = 0;
    return this.grouped(type => {
      const created = aggregateByName(type, results[type]);
      return items.filter(item => item.type === type).map(item => {
        const position = index++;
        const result = item.name ? created.find(row => row.primary?.additionalData?.name === item.name) : undefined;
        if (position < shown && install && result && item.state === 'DONE') {
          return this.row(type, result, item);
        }
        const name = item.name ?? this.translate.instant('solution-creator.pane.pending-' + type, {profile: item.profile});
        return this.row(type, {
          key: item.name ? `${type}:${item.name}` : `${type}:pending:${item.profile}`,
          primary: {additionalData: {name}} as EntityResult,
          status: IssueStatus.OK,
          errors: [],
          warnings: []
        }, item, position < shown ? undefined : position === shown ? 'now' : 'wait');
      });
    });
  }

  private grouped(rowsOf: (type: EntityType) => InstalledRow[]): InstalledSection[] {
    return GROUPS
      .map(group => ({id: group.id, title: group.title, link: group.link, rows: group.types.flatMap(type => rowsOf(type))}))
      .filter(section => section.rows.length);
  }

  private row(type: EntityType, row: AggregatedEntityResult, item?: SolutionProgressItem, progress?: 'wait' | 'now'): InstalledRow {
    const kind = aiEntityKind(type === EntityType.ALARM ? 'ALARM_RULE' : type === EntityType.ROLE ? EntityType.USER : type);
    const details = item && !item.name ? {} : this.detailsOf(type, row.primary?.additionalData ?? {}, item);
    return {...row, type, icon: kind.icon, palette: kind.palette, details, progress};
  }

  private detailsOf(type: EntityType, data: Record<string, any>, item?: SolutionProgressItem): RowDetails {
    const solution = this.solution()?.data;
    const name: string = item?.name ?? data.name;
    switch (type) {
      case EntityType.DASHBOARD:
        return {role: solution?.dashboardsOverview?.find(dashboard => dashboard.name === name)?.assignedTo};
      case EntityType.DEVICE:
      case EntityType.ASSET:
      case EntityType.CUSTOMER: {
        const profile = item?.profile ?? data.type ?? this.entityOf(type, name)?.profile;
        const chipKind = aiEntityKind(type === EntityType.DEVICE ? EntityType.DEVICE_PROFILE : type);
        return profile ? {chip: {label: profile, icon: chipKind.icon, palette: chipKind.palette}} : {};
      }
      case EntityType.USER: {
        const user = this.entityOf(type, name);
        const customer = item ? item.customer : (data.customer ?? user?.customer);
        return {role: item?.profile ?? user?.profile, where: customer || this.translate.instant('tenant.tenant')};
      }
      case EntityType.DEVICE_PROFILE:
        return {kindLabel: this.translate.instant('entity.type-device')};
      case EntityType.ASSET_PROFILE:
        return {kindLabel: this.translate.instant('entity.type-asset')};
      case EntityType.ROLE: {
        const permissions = solution?.solutionDescriptor?.iam?.find(role => role.name === name)?.permissions ?? '';
        return {ops: permissions.toUpperCase().split('').filter(letter => OPERATIONS[letter])
            .map(letter => this.translate.instant('solution-creator.pane.op-' + OPERATIONS[letter]))};
      }
      case EntityType.ALARM:
        return {severities: data.severities?.length ? data.severities
            : solution?.solutionDescriptor?.alarms?.find(alarm => alarm.alarmName === name)?.severities ?? []};
      default:
        return {};
    }
  }

  private entityOf(type: EntityType, name: string): Record<string, any> | undefined {
    const entities = this.solution()?.data?.entities;
    const list: Array<Record<string, any>> = type === EntityType.DEVICE ? entities?.devices
      : type === EntityType.ASSET ? entities?.assets
      : type === EntityType.CUSTOMER ? entities?.customers
      : type === EntityType.USER ? entities?.users : null;
    return list?.find(entity => type === EntityType.USER
      ? [[entity.firstName, entity.lastName].filter(Boolean).join(' '), entity.email].includes(name)
      : entity.name === name);
  }

  link(type: EntityType, row: AggregatedEntityResult): string | null {
    const entity = row.primary;
    if (!entity?.id?.id || row.status === IssueStatus.ERROR) {
      return null;
    }
    if (type === EntityType.ENTITY_GROUP) {
      const prefix = groupUrlPrefixByEntityType.get(entity.additionalData?.groupType as EntityType);
      return prefix ? `${prefix}/${entity.id.id}` : null;
    }
    const base = baseDetailsPageByEntityType.get(entity.id.entityType as EntityType);
    return base ? `${base}/${entity.id.id}` : null;
  }

  severityColor(severity: AlarmSeverity): string {
    return alarmSeverityColors.get(severity) ?? null;
  }

  severityLabel(severity: AlarmSeverity): string {
    return alarmSeverityTranslations.get(severity) ?? severity;
  }

  statusIcon(row: AggregatedEntityResult): string {
    return row.status === IssueStatus.ERROR ? 'close' : row.status === IssueStatus.WARN ? 'priority_high' : 'check';
  }

  statusTone(row: AggregatedEntityResult): string {
    return row.status === IssueStatus.ERROR ? 'error' : row.status === IssueStatus.WARN ? 'warn' : 'ok';
  }

  statusTip(row: AggregatedEntityResult): string {
    const word = this.translate.instant('solution-creator.pane.status-' + this.statusTone(row));
    const messages = [...row.errors, ...row.warnings].map(entry => entry.error?.message).filter(Boolean);
    if (!messages.length) {
      return word;
    }
    return row.status === IssueStatus.WARN ? messages.join('; ') : `${word}: ${messages.join('; ')}`;
  }

  severityTone(severity: AlarmSeverity): string {
    return (severity ?? '').toLowerCase();
  }

  dashboardUser(row: AggregatedEntityResult): string {
    return this.userName(row.primary?.additionalData?.userId);
  }

  canViewAs(row: AggregatedEntityResult): boolean {
    return !!row.primary?.additionalData?.userId && row.status !== IssueStatus.ERROR;
  }

  viewAs(row: AggregatedEntityResult): void {
    const entity: EntityResult = row.primary;
    const userId = entity.additionalData.userId;
    const tenantUser = !this.findUser(userId)?.additionalData?.customer;
    this.authService.redirectUrl = tenantUser ? `/dashboards/all/${entity.id.id}` : `/dashboards/${entity.id.id}`;
    this.authService.loginAsUser(userId).subscribe(() => {});
  }

  private userName(userId: string | undefined): string {
    return userId ? this.findUser(userId)?.additionalData?.name ?? '' : '';
  }

  private findUser(userId: string): EntityResult | undefined {
    return this.solution()?.metadata?.installResult?.entityResults?.[EntityType.USER]?.find(user => user.id?.id === userId);
  }
}
