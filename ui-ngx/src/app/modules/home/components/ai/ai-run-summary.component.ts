// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of, Subscription } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { NotificationService } from '@core/http/notification.service';
import { DeviceService } from '@core/http/device.service';
import { DashboardService } from '@core/http/dashboard.service';
import { CalculatedFieldType } from '@shared/models/calculated-field.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { RequestConfig } from '@core/http/http-utils';
import { AiRecordState } from '@home/components/ai/ai-record.component';

interface AiSummaryRow {
  key: string;
  entityType: string;
  alarmRule: boolean;
  name: string;
  meta: string;
  url?: string;
  state?: AiRecordState;
}

const CONFIG: RequestConfig = {ignoreLoading: true, ignoreErrors: true};

const NOTIFICATION_PAGES: Partial<Record<EntityType, string>> = {
  [EntityType.NOTIFICATION_RULE]: '/notification/rules',
  [EntityType.NOTIFICATION_TEMPLATE]: '/notification/templates',
  [EntityType.NOTIFICATION_TARGET]: '/notification/recipients'
};

const PROFILE_TYPES = new Set<string>([EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE]);

@Component({
  selector: 'tb-ai-run-summary',
  template: `
    @if (rows().length) {
      <div class="k-recs" role="list" [attr.aria-label]="'ai-assistant.summary.made' | translate">
        @for (row of rows(); track row.key) {
          <tb-ai-record role="listitem" [entityType]="row.entityType" [alarmRule]="row.alarmRule" [name]="row.name"
                        [meta]="row.meta" [url]="row.url" [state]="row.state"></tb-ai-record>
        }
      </div>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiRunSummaryComponent {

  entities = input.required<EntityId[]>();

  rows = signal<AiSummaryRow[]>([]);

  private key = computed(() => this.entities().map(entity => `${entity.entityType}:${entity.id}`).join(','));

  private entityService = inject(EntityService);
  private calculatedFieldsService = inject(CalculatedFieldsService);
  private notificationService = inject(NotificationService);
  private deviceService = inject(DeviceService);
  private dashboardService = inject(DashboardService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  private loading: Subscription;

  constructor() {
    effect(() => {
      this.key();
      const entities = untracked(() => this.entities());
      this.loading?.unsubscribe();
      this.loading = forkJoin(entities.map(entity => this.row(entity).pipe(catchError(() => of(null)))))
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(rows => this.rows.set(rows.filter(row => !!row)));
    });
  }

  private row(entity: EntityId): Observable<AiSummaryRow | null> {
    const entityType = entity.entityType as EntityType;
    const key = `${entityType}:${entity.id}`;
    const url = getEntityDetailsPageURL(entity.id, entityType) || undefined;
    switch (entityType) {
      case EntityType.DEVICE:
        return this.deviceService.getDeviceInfo(entity.id, CONFIG).pipe(map(device => ({
          key, entityType, alarmRule: false, name: device.name, url,
          meta: this.meta(this.typeLabel(entityType), device.type),
          state: {
            tone: device.active ? 'ok' : 'off',
            label: this.translate.instant(device.active ? 'ai-assistant.record.active' : 'ai-assistant.record.inactive')
          }
        })));
      case EntityType.DASHBOARD:
        return this.dashboardService.getDashboard(entity.id, CONFIG).pipe(map(dashboard => {
          const widgets = Object.keys(dashboard.configuration?.widgets ?? {}).length;
          return {
            key, entityType, alarmRule: false, name: dashboard.title, url,
            meta: this.meta(this.typeLabel(entityType), widgets ? this.translate.instant('ai-assistant.record.widgets', {count: widgets}) : null)
          };
        }));
      case EntityType.CALCULATED_FIELD:
        return this.calculatedFieldsService.getCalculatedFieldById(entity.id, CONFIG).pipe(switchMap(field => {
          const alarmRule = field.type === CalculatedFieldType.ALARM;
          const type = this.translate.instant(alarmRule ? 'ai-assistant.review.type.ALARM_RULE' : 'entity.type-calculated-field');
          const row = (target?: string): AiSummaryRow => ({
            key, entityType, alarmRule, name: field.name, meta: this.meta(type, target),
            url: alarmRule ? `/alarms/alarm-rules/${entity.id}` : `/calculatedFields/${entity.id}`
          });
          const owner = field.entityId;
          if (!owner?.id) {
            return of(row());
          }
          return this.entityService.getEntity(owner.entityType as EntityType, owner.id, CONFIG).pipe(
            map(found => row(found?.name ? this.onTarget(owner.entityType, found.name) : undefined)),
            catchError(() => of(row()))
          );
        }));
      case EntityType.NOTIFICATION_RULE:
        return this.notificationService.getNotificationRuleById(entity.id, CONFIG)
          .pipe(map(rule => this.simpleRow(key, entityType, rule.name, NOTIFICATION_PAGES[entityType])));
      case EntityType.NOTIFICATION_TEMPLATE:
        return this.notificationService.getNotificationTemplateById(entity.id, CONFIG)
          .pipe(map(template => this.simpleRow(key, entityType, template.name, NOTIFICATION_PAGES[entityType])));
      default:
        return this.entityService.getEntity(entityType, entity.id, CONFIG).pipe(map(found => {
          const row = this.simpleRow(key, entityType, found?.name, NOTIFICATION_PAGES[entityType] ?? url);
          const subtype = (found as {type?: string})?.type;
          if (row && subtype && entityType === EntityType.ASSET) {
            row.meta = this.meta(row.meta, subtype);
          }
          return row;
        }));
    }
  }

  private simpleRow(key: string, entityType: EntityType, name: string, url?: string): AiSummaryRow | null {
    return name ? {key, entityType, alarmRule: false, name, meta: this.typeLabel(entityType), url} : null;
  }

  private onTarget(entityType: string, name: string): string {
    return this.translate.instant(PROFILE_TYPES.has(entityType) ? 'ai-assistant.record.on-profile' : 'ai-assistant.record.on', {name});
  }

  private meta(...parts: Array<string | null | undefined>): string {
    return parts.filter(part => !!part).join(' · ');
  }

  private typeLabel(entityType: EntityType): string {
    const translation = entityTypeTranslations.get(entityType)?.type;
    return translation ? this.translate.instant(translation) : entityType;
  }
}
