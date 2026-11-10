// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EntityService } from '@core/http/entity.service';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { RequestConfig } from '@core/http/http-utils';
import { AiStartedFrom } from '@shared/models/solution-creator.models';
import { aiEntityKind } from '@home/components/ai/ai-action.models';

interface AiPage {
  name: string;
  url?: string;
}

const CONFIG: RequestConfig = {ignoreLoading: true, ignoreErrors: true};

const LIST_PAGES: Record<string, {entityType: string; url: string}> = {
  DEVICE_LIST: {entityType: EntityType.DEVICE, url: '/entities/devices'},
  DEVICE_GROUP_LIST: {entityType: EntityType.DEVICE, url: '/entities/devices/groups'},
  DASHBOARD_LIST: {entityType: EntityType.DASHBOARD, url: '/dashboards'},
  ALARM_LIST: {entityType: 'ALARM', url: '/alarms/alarms'},
  ALARM_RULE_LIST: {entityType: 'ALARM_RULE', url: '/alarms/alarm-rules'},
  CALCULATED_FIELD_LIST: {entityType: EntityType.CALCULATED_FIELD, url: '/calculatedFields'},
  ASSET_LIST: {entityType: EntityType.ASSET, url: '/entities/assets/all'},
  ASSET_GROUP_LIST: {entityType: EntityType.ASSET, url: '/entities/assets/groups'},
  ENTITY_VIEW_LIST: {entityType: EntityType.ENTITY_VIEW, url: '/entities/entityViews/all'},
  DEVICE_PROFILE_LIST: {entityType: EntityType.DEVICE_PROFILE, url: '/profiles/deviceProfiles'},
  ASSET_PROFILE_LIST: {entityType: EntityType.ASSET_PROFILE, url: '/profiles/assetProfiles'},
  CUSTOMER_LIST: {entityType: EntityType.CUSTOMER, url: '/customers/all'},
  USER_LIST: {entityType: EntityType.USER, url: '/users/all'},
  RULE_CHAIN_LIST: {entityType: EntityType.RULE_CHAIN, url: '/ruleChains'},
  INTEGRATION_LIST: {entityType: EntityType.INTEGRATION, url: '/integrationsCenter/integrations'},
  CONVERTER_LIST: {entityType: EntityType.CONVERTER, url: '/integrationsCenter/converters'},
  NOTIFICATION_TEMPLATE_LIST: {entityType: EntityType.NOTIFICATION_TEMPLATE, url: '/notification/templates'},
  NOTIFICATION_RULE_LIST: {entityType: EntityType.NOTIFICATION_RULE, url: '/notification/rules'},
  NOTIFICATION_RECIPIENT_LIST: {entityType: EntityType.NOTIFICATION_TARGET, url: '/notification/recipients'},
  SENT_NOTIFICATION_LIST: {entityType: EntityType.NOTIFICATION_RULE, url: '/notification/sent'},
  NOTIFICATION_INBOX: {entityType: EntityType.NOTIFICATION_RULE, url: '/notification/inbox'}
};

@Component({
  selector: 'tb-ai-started-from',
  template: `
    @if (page(); as current) {
      <span>{{ 'ai-assistant.started-from' | translate }}</span>
      @if (current.url) {
        <a class="tb-chiplink" [routerLink]="current.url" [attr.title]="'ai-assistant.open-page' | translate: {name: current.name}">
          <code class="k-ent" [attr.data-pal]="kind().palette"><mat-icon class="k-ent-ic" aria-hidden="true">{{ kind().icon }}</mat-icon>{{ current.name }}</code>
        </a>
      } @else {
        <code class="k-ent" [attr.data-pal]="kind().palette"><mat-icon class="k-ent-ic" aria-hidden="true">{{ kind().icon }}</mat-icon>{{ current.name }}</code>
      }
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'k-start',
    'role': 'note',
    '[hidden]': '!page()'
  }
})
export class AiStartedFromComponent {

  from = input.required<AiStartedFrom>();

  page = signal<AiPage | null>(null);

  kind = computed(() => {
    const from = this.from();
    const entityType = from.entityId?.entityType ?? LIST_PAGES[from.type]?.entityType ?? from.type;
    return aiEntityKind(entityType, from.type === 'ALARM_RULE' || from.type === 'ALARM_RULE_LIST');
  });

  private entityService = inject(EntityService);
  private calculatedFieldsService = inject(CalculatedFieldsService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  private loading: Subscription;

  constructor() {
    effect(() => {
      const from = this.from();
      untracked(() => {
        this.loading?.unsubscribe();
        this.loading = this.resolve(from).pipe(
          catchError(() => of(null)),
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(page => this.page.set(page));
      });
    });
  }

  private resolve(from: AiStartedFrom): Observable<AiPage | null> {
    const list = LIST_PAGES[from.type];
    if (list) {
      return of({name: this.translate.instant('ai-assistant.page.' + from.type), url: list.url});
    }
    const entityId = from.entityId;
    if (!entityId?.id) {
      return of(null);
    }
    if (entityId.entityType === EntityType.CALCULATED_FIELD) {
      const alarmRule = from.type === 'ALARM_RULE';
      return this.calculatedFieldsService.getCalculatedFieldById(entityId.id, CONFIG).pipe(map(field => ({
        name: field.name,
        url: alarmRule ? `/alarms/alarm-rules/${entityId.id}` : `/calculatedFields/${entityId.id}`
      })));
    }
    const entityType = entityId.entityType as EntityType;
    return this.entityService.getEntity(entityType, entityId.id, CONFIG).pipe(map(entity => {
      const named = entity as {name?: string; title?: string};
      const name = named?.title || named?.name;
      return name ? {name, url: getEntityDetailsPageURL(entityId.id, entityType) || undefined} : null;
    }));
  }
}
