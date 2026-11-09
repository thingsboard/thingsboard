// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, DestroyRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
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
  type: EntityType;
  title: string;
  icon: string;
  palette: string;
  rows: InstalledRow[];
}

type InstalledRow = AggregatedEntityResult & {progress?: 'wait' | 'now'};

const SECTIONS: EntityType[] = [
  EntityType.DASHBOARD,
  EntityType.DEVICE,
  EntityType.ASSET,
  EntityType.CUSTOMER,
  EntityType.USER,
  EntityType.DEVICE_PROFILE,
  EntityType.ASSET_PROFILE,
  EntityType.ROLE,
  EntityType.ENTITY_GROUP,
  EntityType.CALCULATED_FIELD,
  EntityType.ALARM,
  EntityType.RULE_CHAIN
];

const SUPPORTING: EntityType[] = [EntityType.ENTITY_GROUP, EntityType.RULE_CHAIN];

const GENERATED: EntityType[] = [EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.USER];

const STEP_MS = 160;

@Component({
  selector: 'tb-solution-installed',
  templateUrl: './solution-installed.component.html',
  styleUrls: ['./solution-pane.scss', './solution-installed.component.scss'],
  standalone: false
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
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
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
    return SECTIONS
      .map(type => {
        const kind = aiEntityKind(type === EntityType.ALARM ? 'ALARM_RULE' : type === EntityType.ROLE ? EntityType.USER : type);
        return {
          type,
          title: entityTypeTranslations.get(type)?.typePlural ?? type,
          icon: kind.icon,
          palette: kind.palette,
          rows: aggregateByName(type, results[type])
            .filter(row => !SUPPORTING.includes(type) || row.status !== IssueStatus.OK)
        };
      })
      .filter(section => section.rows.length);
  });

  entityCount = computed(() => this.sections()
    .filter(section => section.type !== EntityType.DASHBOARD)
    .reduce((count, section) => count + section.rows.length, 0));

  dashboardCount = computed(() => this.sections().find(section => section.type === EntityType.DASHBOARD)?.rows.length ?? 0);

  hasMain = computed(() => !!this.solution()?.metadata?.installResult?.mainDashboardId);

  private plannedSections(items: SolutionProgressItem[], results: Partial<Record<EntityType, EntityResult[]>>): InstalledSection[] {
    const shown = Math.max(0, this.shown());
    const install = this.stage() === 'INSTALL';
    let index = 0;
    return SECTIONS
      .map(type => {
        const kind = aiEntityKind(type === EntityType.ALARM ? 'ALARM_RULE' : type === EntityType.ROLE ? EntityType.USER : type);
        const created = aggregateByName(type, results[type]);
        const rows: InstalledRow[] = items.filter(item => item.type === type).map(item => {
          const position = index++;
          const result = item.name ? created.find(row => row.primary?.additionalData?.name === item.name) : undefined;
          if (position < shown && install && result && item.state === 'DONE') {
            return result;
          }
          return {
            key: item.name ? `${type}:${item.name}` : `${type}:pending:${item.profile}`,
            primary: {additionalData: {name: item.name ?? this.translate.instant('solution-creator.pane.pending-' + type, {profile: item.profile})}} as EntityResult,
            status: IssueStatus.OK,
            errors: [],
            warnings: [],
            progress: position < shown ? undefined : position === shown ? 'now' : 'wait'
          };
        });
        return {type, title: entityTypeTranslations.get(type)?.typePlural ?? type, icon: kind.icon, palette: kind.palette, rows};
      })
      .filter(section => section.rows.length);
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

  detail(type: EntityType, row: AggregatedEntityResult): string {
    const data = row.primary?.additionalData;
    switch (type) {
      case EntityType.DASHBOARD:
        return this.userName(data?.userId);
      case EntityType.USER:
        return [data?.customer || this.translate.instant('tenant.tenant'), data?.email].filter(Boolean).join(' · ');
      case EntityType.ALARM:
        return data?.condition ?? '';
      case EntityType.ENTITY_GROUP:
        return data?.groupType ? this.translate.instant(entityTypeTranslations.get(data.groupType as EntityType)?.typePlural ?? data.groupType) : '';
      default:
        return data?.type ?? data?.customer ?? '';
    }
  }

  severities(row: AggregatedEntityResult): AlarmSeverity[] {
    return row.primary?.additionalData?.severities ?? [];
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
    return messages.length ? `${word}: ${messages.join('; ')}` : word;
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
