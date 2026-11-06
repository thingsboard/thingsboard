// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  untracked,
  viewChild
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
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
  image?: string;
  widgets?: number;
}

const TIGHT_WIDTH = 400;

function dashboardThumb(cards: number): string {
  const n = Math.max(1, Math.min(cards, 6));
  const w = (112 - (n - 1) * 3) / n;
  const tiles = Array.from({length: n}, (_, i) => `<rect x="${4 + i * (w + 3)}" y="12" width="${w.toFixed(1)}" height="18" rx="2" fill="#fff" stroke="#e3e6e8"/>`
    + `<rect x="${7 + i * (w + 3)}" y="16" width="${(w * 0.45).toFixed(1)}" height="2.5" rx="1" fill="#c9cfd3"/>`
    + `<rect x="${7 + i * (w + 3)}" y="22" width="${(w * 0.6).toFixed(1)}" height="4" rx="1" fill="#5f6b73"/>`).join('');
  const rows = [40, 48, 56, 64].map(y => `<rect x="84" y="${y}" width="5" height="3" rx="1" fill="${y === 40 ? '#d12730' : '#c9cfd3'}"/>`
    + `<rect x="91" y="${y}" width="21" height="3" rx="1" fill="#dde1e4"/>`).join('');
  return `<svg viewBox="0 0 120 80" xmlns="http://www.w3.org/2000/svg"><rect width="120" height="80" fill="#f4f6f7"/>`
    + `<rect x="4" y="4" width="40" height="4" rx="1" fill="#9aa3a9"/>${tiles}`
    + `<rect x="4" y="33" width="74" height="43" rx="2" fill="#fff" stroke="#e3e6e8"/>`
    + `<polyline points="8,64 16,60 24,62 32,54 40,57 48,50 56,52 64,46 74,49" fill="none" stroke="#305680" stroke-width="1.4" stroke-linejoin="round"/>`
    + `<rect x="81" y="33" width="35" height="43" rx="2" fill="#fff" stroke="#e3e6e8"/>${rows}</svg>`;
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
    @if (lead(); as card) {
      <div #card class="tb-rec card type-dashboard primary with-pic" data-pal="view" [class.tight]="tight()">
        <div class="tb-rec-row">
          <span class="tb-rec-pic" aria-hidden="true">
            @if (card.image) {
              <img [src]="card.image" alt="">
            } @else {
              <span class="tb-rec-img" [innerHTML]="thumb()"></span>
            }
            <span class="tb-rec-tile"><mat-icon aria-hidden="true">dashboard</mat-icon></span>
          </span>
          <div class="tb-rec-body">
            <a class="tb-rec-title" [routerLink]="card.url">{{ card.name }}</a>
            <span class="tb-rec-meta">{{ leadMeta() || card.meta }}</span>
          </div>
          <span class="tb-rec-side">
            <a mat-flat-button class="tb-open-btn" [routerLink]="card.url">
              <mat-icon aria-hidden="true">dashboard</mat-icon>{{ 'ai-assistant.summary.open-dashboard' | translate }}
            </a>
          </span>
        </div>
      </div>
    }
    @if (rest().length) {
      <div class="k-recs" role="list" [attr.aria-label]="'ai-assistant.summary.made' | translate">
        @for (row of rest(); track row.key) {
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
  leadMeta = input<string | null>(null);

  rows = signal<AiSummaryRow[]>([]);

  lead = computed(() => this.rows().find(row => row.entityType === EntityType.DASHBOARD && !!row.url) ?? null);

  rest = computed(() => this.rows().filter(row => row !== this.lead()));

  thumb = computed(() => this.sanitizer.bypassSecurityTrustHtml(dashboardThumb(this.lead()?.widgets ?? 4)));

  tight = signal(false);

  private card = viewChild<ElementRef<HTMLElement>>('card');

  private sanitizer = inject(DomSanitizer);

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
    effect(onCleanup => {
      const card = this.card()?.nativeElement;
      if (!card || typeof ResizeObserver === 'undefined') {
        return;
      }
      const observer = new ResizeObserver(() => this.tight.set(card.clientWidth > 0 && card.clientWidth < TIGHT_WIDTH));
      observer.observe(card);
      onCleanup(() => observer.disconnect());
    });
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
            key, entityType, alarmRule: false, name: dashboard.title, url, widgets, image: dashboard.image || undefined,
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
