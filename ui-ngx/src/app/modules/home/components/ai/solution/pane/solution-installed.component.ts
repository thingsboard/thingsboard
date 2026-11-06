// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { EntityResult, SolutionCreatorInfo } from '@shared/models/solution-creator.models';
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
  rows: AggregatedEntityResult[];
}

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

@Component({
  selector: 'tb-solution-installed',
  templateUrl: './solution-installed.component.html',
  styleUrls: ['./solution-pane.scss', './solution-installed.component.scss'],
  standalone: false
})
export class SolutionInstalledComponent {

  solution = input.required<SolutionCreatorInfo>();
  creating = input(false);

  @Output() openMain = new EventEmitter<void>();

  private translate = inject(TranslateService);
  private authService = inject(AuthService);

  bannerClosed = signal(false);

  installed = computed(() => !!this.solution()?.metadata?.installed);

  sections = computed<InstalledSection[]>(() => {
    const results = this.solution()?.metadata?.installResult?.entityResults;
    if (!results) {
      return [];
    }
    return SECTIONS
      .map(type => {
        const kind = aiEntityKind(type === EntityType.ALARM ? 'ALARM_RULE' : type);
        return {
          type,
          title: entityTypeTranslations.get(type)?.typePlural ?? type,
          icon: kind.icon,
          palette: kind.palette,
          rows: aggregateByName(type, results[type])
        };
      })
      .filter(section => section.rows.length);
  });

  entityCount = computed(() => this.sections()
    .filter(section => section.type !== EntityType.DASHBOARD)
    .reduce((count, section) => count + section.rows.length, 0));

  dashboardCount = computed(() => this.sections().find(section => section.type === EntityType.DASHBOARD)?.rows.length ?? 0);

  hasMain = computed(() => !!this.solution()?.metadata?.installResult?.mainDashboardId);

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
