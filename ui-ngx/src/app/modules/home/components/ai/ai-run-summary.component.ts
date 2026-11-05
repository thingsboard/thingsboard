// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { NotificationService } from '@core/http/notification.service';
import { CalculatedFieldType } from '@shared/models/calculated-field.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { RequestConfig } from '@core/http/http-utils';

interface AiSummaryRow {
  key: string;
  icon: string;
  name: string;
  meta: string;
  url?: string;
}

const CONFIG: RequestConfig = {ignoreLoading: true, ignoreErrors: true};

const ICONS: Partial<Record<EntityType, string>> = {
  [EntityType.DEVICE]: 'memory',
  [EntityType.ASSET]: 'domain',
  [EntityType.DASHBOARD]: 'dashboard',
  [EntityType.CALCULATED_FIELD]: 'functions',
  [EntityType.NOTIFICATION_RULE]: 'notifications_active',
  [EntityType.NOTIFICATION_TEMPLATE]: 'description',
  [EntityType.NOTIFICATION_TARGET]: 'group'
};

const NOTIFICATION_PAGES: Partial<Record<EntityType, string>> = {
  [EntityType.NOTIFICATION_RULE]: '/notification/rules',
  [EntityType.NOTIFICATION_TEMPLATE]: '/notification/templates',
  [EntityType.NOTIFICATION_TARGET]: '/notification/recipients'
};

@Component({
  selector: 'tb-ai-run-summary',
  templateUrl: './ai-run-summary.component.html',
  styleUrls: ['./ai-run-summary.component.scss'],
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
    const icon = ICONS[entityType] ?? 'category';
    switch (entityType) {
      case EntityType.CALCULATED_FIELD:
        return this.calculatedFieldsService.getCalculatedFieldById(entity.id, CONFIG).pipe(map(field => {
          const alarmRule = field.type === CalculatedFieldType.ALARM;
          return {
            key,
            icon: alarmRule ? 'rule' : icon,
            name: field.name,
            meta: this.translate.instant(alarmRule ? 'ai-assistant.summary.alarm-rule' : 'ai-assistant.summary.calculated-field'),
            url: alarmRule ? `/alarms/alarm-rules/${entity.id}` : `/calculatedFields/${entity.id}`
          };
        }));
      case EntityType.NOTIFICATION_RULE:
        return this.notificationService.getNotificationRuleById(entity.id, CONFIG)
          .pipe(map(rule => this.simpleRow(key, icon, rule.name, entityType, NOTIFICATION_PAGES[entityType])));
      case EntityType.NOTIFICATION_TEMPLATE:
        return this.notificationService.getNotificationTemplateById(entity.id, CONFIG)
          .pipe(map(template => this.simpleRow(key, icon, template.name, entityType, NOTIFICATION_PAGES[entityType])));
      default:
        return this.entityService.getEntity(entityType, entity.id, CONFIG).pipe(map(found => {
          const row = this.simpleRow(key, icon, found?.name, entityType,
            NOTIFICATION_PAGES[entityType] ?? (getEntityDetailsPageURL(entity.id, entityType) || undefined));
          const subtype = (found as {type?: string})?.type;
          if (subtype && (entityType === EntityType.DEVICE || entityType === EntityType.ASSET)) {
            row.meta = `${row.meta} · ${subtype}`;
          }
          return row;
        }));
    }
  }

  private simpleRow(key: string, icon: string, name: string, entityType: EntityType, url?: string): AiSummaryRow | null {
    if (!name) {
      return null;
    }
    const translation = entityTypeTranslations.get(entityType)?.type;
    return {
      key,
      icon,
      name,
      meta: translation ? this.translate.instant(translation) : entityType,
      url
    };
  }
}
