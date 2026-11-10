// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { afterRenderEffect, ChangeDetectionStrategy, Component, computed, DestroyRef, effect, ElementRef, inject, input, signal, untracked } from '@angular/core';
import { ClipboardService } from 'ngx-clipboard';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { getEntityDetailsPageURL } from '@core/utils';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { codeTabsBlock, decorateCodeBlocks } from '@home/components/ai/ai-code-block';
import {
  AiDetailBlock,
  AiDetailEntity,
  AiDetailFact,
  AiDetailNames,
  AiDetailRecord,
  AiDetailText,
  AiDetailView
} from '@home/components/ai/ai-action-detail.models';

const FETCHED = new Set<string>([
  EntityType.DEVICE, EntityType.ASSET, EntityType.DASHBOARD, EntityType.CUSTOMER, EntityType.USER, EntityType.DEVICE_PROFILE,
  EntityType.ASSET_PROFILE, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.RULE_CHAIN
]);

const THINGS: Record<string, string> = {
  DASHBOARD_WIDGET: 'widget',
  DASHBOARD_STATE: 'state',
  DASHBOARD_ENTITY_ALIAS: 'alias',
  DASHBOARD_FILTER: 'filter',
  ALARM_RULE: 'alarm-rule',
  CALCULATED_FIELD: 'calculated-field',
  NOTIFICATION_RULE: 'notification-rule',
  NOTIFICATION_TARGET: 'notification-target',
  NOTIFICATION_TEMPLATE: 'notification-template'
};

const MAX_HUB = 10;

const PROTOCOL_KEY = 'tb-ai-code-protocol';

interface HubRow {
  id: string;
  name: string;
  maker?: string;
  installs?: string;
  description?: string;
  verified?: boolean;
  item?: MpItemVersionView;
}

@Component({
  selector: 'tb-ai-action-detail',
  templateUrl: './ai-action-detail.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'k-detail'
  }
})
export class AiActionDetailComponent {

  view = input.required<AiDetailView>();

  names = input<AiDetailNames>(new Map());

  private translate = inject(TranslateService);
  private entityService = inject(EntityService);
  private iotHubApi = inject(IotHubApiService);
  private iotHubActions = inject(IotHubActionsService, {optional: true});
  private destroyRef = inject(DestroyRef);
  private host = inject(ElementRef<HTMLElement>);
  private clipboard = inject(ClipboardService);

  private fetched = signal<ReadonlyMap<string, string>>(new Map());

  private hubItems = signal<ReadonlyMap<string, MpItemVersionView>>(new Map());

  revealed = signal<ReadonlySet<string>>(new Set());

  hood = computed(() => {
    const hood = this.view().hood;
    this.fetched();
    return hood ? this.say(hood) : '';
  });

  hub = computed<HubRow[]>(() => {
    const block = this.view().blocks.find(item => item.type === 'hub');
    const items = this.hubItems();
    if (block?.type !== 'hub') {
      return [];
    }
    return block.rows.map(row => {
      const item = items.get(row.id);
      const installs = item?.totalInstallCount ?? item?.installCount;
      return {
        id: row.id,
        name: item?.name ?? row.name,
        maker: item?.creatorDisplayName ?? row.vendor,
        installs: installs != null ? this.translate.instant('ai-assistant.detail.installs', {count: installs.toLocaleString()}) : undefined,
        description: row.description ?? item?.description,
        verified: item?.creatorVerified,
        item
      };
    });
  });

  constructor() {
    effect(() => {
      const view = this.view();
      untracked(() => {
        this.resolveNames(view);
        this.loadHub(view);
      });
    });
    afterRenderEffect(() => {
      const view = this.view();
      const copy = (text: string) => this.clipboard.copyFromContent(text);
      decorateCodeBlocks(this.host.nativeElement, this.translate, copy);
      this.host.nativeElement.querySelectorAll('.k-codetabs-slot:empty').forEach((slot: HTMLElement) => {
        const block = view.blocks[Number(slot.dataset.block)];
        if (block?.type === 'commands' && block.rows.length) {
          const remembered = this.rememberedTab();
          const initial = Math.max(block.rows.findIndex(row => row.label === remembered), 0);
          slot.append(codeTabsBlock(block.rows, 'bash', this.translate, copy, initial, tab => this.rememberTab(tab.label)));
        }
      });
    });
  }

  say(text: AiDetailText): string {
    const params: Record<string, string | number> = {};
    for (const [key, value] of Object.entries(text.params ?? {})) {
      params[key] = typeof value === 'object' ? this.entityText(value) : typeof value === 'string' && value.startsWith('ai-assistant.') ? this.translate.instant(value) : value;
    }
    return this.translate.instant(text.key, params);
  }

  label(fact: AiDetailFact): string {
    return fact.raw ? fact.label : this.translate.instant(fact.label);
  }

  factValue(fact: AiDetailFact): string {
    if (fact.entity) {
      return this.nameOf(fact.entity) ?? fact.entity.id ?? '—';
    }
    return fact.value?.startsWith('ai-assistant.') ? this.translate.instant(fact.value) : fact.value ?? '';
  }

  factLink(fact: AiDetailFact): string | null {
    return fact.entity ? this.urlOf(fact.entity.id, fact.entity.entityType) : null;
  }

  masked(value: string): string {
    return value.length <= 6 ? '••••••' : '••••••••' + value.slice(-4);
  }

  reveal(key: string): void {
    this.revealed.update(set => new Set(set).add(key));
  }

  recordUrl(record: AiDetailRecord): string | null {
    return this.urlOf(record.id, record.entityType);
  }

  kind(entityType: string) {
    return entityType === 'IMAGE' ? {icon: 'image', palette: 'view'} : aiEntityKind(entityType, entityType === 'ALARM_RULE');
  }

  more(block: AiDetailBlock): number {
    return block.type === 'records' && block.total ? Math.max(block.total - block.rows.length, 0) : 0;
  }

  openHub(row: HubRow, event: MouseEvent): void {
    if (!row.item || !this.iotHubActions) {
      return;
    }
    event.preventDefault();
    this.iotHubActions.openItemDetail(row.item).subscribe();
  }

  time(ts: number): string {
    return new Date(ts).toLocaleString([], {dateStyle: 'short', timeStyle: 'short'});
  }

  number(value: number): string {
    return Number.isInteger(value) ? String(value) : String(Math.round(value * 100) / 100);
  }

  private rememberedTab(): string | null {
    try {
      return localStorage.getItem(PROTOCOL_KEY);
    } catch {
      return null;
    }
  }

  private rememberTab(label: string): void {
    try {
      localStorage.setItem(PROTOCOL_KEY, label);
    } catch {
    }
  }

  private entityText(entity: AiDetailEntity): string {
    const name = this.nameOf(entity);
    if (name) {
      return `'${name}'`;
    }
    return this.translate.instant('ai-assistant.detail.this', {what: this.typeWord(entity.entityType)});
  }

  private typeWord(entityType?: string): string {
    if (entityType && THINGS[entityType]) {
      return this.translate.instant('ai-assistant.detail.thing.' + THINGS[entityType]);
    }
    const key = entityType ? entityTypeTranslations.get(entityType as EntityType)?.type : undefined;
    return key ? this.translate.instant(key).toLocaleLowerCase() : this.translate.instant('ai-assistant.detail.thing.entity');
  }

  private nameOf(entity: AiDetailEntity): string | undefined {
    return entity.name ?? (entity.id ? this.names().get(entity.id)?.name ?? this.fetched().get(entity.id) : undefined);
  }

  private urlOf(id?: string, entityType?: string): string | null {
    return id && entityType && FETCHED.has(entityType) ? getEntityDetailsPageURL(id, entityType as EntityType) || null : null;
  }

  private resolveNames(view: AiDetailView): void {
    const wanted = new Map<string, string>();
    const want = (entity?: AiDetailEntity) => {
      if (entity?.id && !entity.name && entity.entityType && FETCHED.has(entity.entityType) && !this.names().has(entity.id) && !this.fetched().has(entity.id)) {
        wanted.set(entity.id, entity.entityType);
      }
    };
    Object.values(view.hood?.params ?? {}).forEach(value => typeof value === 'object' && want(value));
    view.blocks.forEach(block => block.type === 'facts' && block.rows.forEach(fact => want(fact.entity)));
    if (!wanted.size) {
      return;
    }
    const config = {ignoreLoading: true, ignoreErrors: true};
    forkJoin([...wanted].map(([id, type]) => this.entityService.getEntity(type as EntityType, id, config).pipe(
      map(found => [id, (found as {name?: string; title?: string})?.name ?? (found as {title?: string})?.title] as [string, string]),
      catchError(() => of([id, undefined] as [string, string]))
    ))).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(found => {
      const next = new Map(this.fetched());
      found.filter(([, name]) => !!name).forEach(([id, name]) => next.set(id, name));
      this.fetched.set(next);
    });
  }

  private loadHub(view: AiDetailView): void {
    const block = view.blocks.find(item => item.type === 'hub');
    if (block?.type !== 'hub' || !this.iotHubApi.baseUrl) {
      return;
    }
    const ids = block.rows.slice(0, MAX_HUB).map(row => row.id).filter(id => !this.hubItems().has(id));
    if (!ids.length) {
      return;
    }
    const config = {ignoreLoading: true, ignoreErrors: true};
    forkJoin(ids.map(id => this.iotHubApi.getVersionInfo(id, config).pipe(catchError(() => of(null))))).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(items => {
        const next = new Map(this.hubItems());
        items.filter(Boolean).forEach(item => next.set(item.id, item));
        this.hubItems.set(next);
      });
  }
}
