// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { aiRecordKey, AiRecordView, AiShownRecords } from '@home/components/ai/ai-record.models';
import { AiRecordLoaderService } from '@home/components/ai/ai-record-loader.service';
import { AiCardAction } from '@home/components/ai/ai-card-actions.component';
import { AiRecordPrimary } from '@home/components/ai/ai-record-card.component';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';

const LIST_SHOWN = 8;

const TARGETED_HUB_TYPES = new Set<string>([ItemType.CALCULATED_FIELD, ItemType.ALARM_RULE]);

@Component({
  selector: 'tb-ai-records',
  template: `
    @if (views().length === 1 && single(); as rec) {
      <tb-ai-record-card [view]="rec" [primary]="primary()" [facts]="shown().facts" [note]="shown().note"
                         [actions]="cardActions()" (compose)="compose.emit($event)"></tb-ai-record-card>
    } @else if (views().length > 1) {
      <div class="tb-list" [class.open]="listOpen()" [attr.data-pal]="listKind()?.palette ?? null">
        <button type="button" class="tb-list-head" [attr.aria-expanded]="listOpen()" (click)="toggleList()">
          <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ listKind()?.icon ?? 'interests' }}</mat-icon></span>
          <span class="tb-list-label">{{ listLabel() }}</span>
          <mat-icon class="tb-list-chev" aria-hidden="true">expand_more</mat-icon>
        </button>
        @if (listOpen()) {
          <div class="tb-list-body" role="list" [attr.aria-label]="listLabel()">
            @for (rec of shownViews(); track rec.key) {
              <tb-ai-record role="listitem" [entityType]="rec.entityType" [alarmRule]="rec.alarmRule" [name]="rec.name"
                            [meta]="sameKind() ? lineMeta(rec) : rec.meta" [url]="rec.url" [state]="rec.state" [alarm]="rec.alarm"
                            [hub]="rec.hub"></tb-ai-record>
            }
            @if (views().length > LIST_SHOWN) {
              <button type="button" class="tb-more" [attr.aria-expanded]="allShown()" (click)="allShown.set(!allShown())">
                <mat-icon class="tb-mat-16" aria-hidden="true">{{ allShown() ? 'expand_less' : 'expand_more' }}</mat-icon>
                {{ allShown() ? ('ai-assistant.summary.fewer' | translate) : ('ai-assistant.summary.more' | translate: {count: views().length - LIST_SHOWN}) }}
              </button>
            }
          </div>
        }
      </div>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiRecordsComponent {

  shown = input.required<AiShownRecords>();

  compose = output<string>();

  made = input<string[]>([]);

  views = signal<AiRecordView[]>([]);

  allShown = signal(false);

  private listToggled = signal<boolean | null>(null);

  listOpen = computed(() => this.listToggled() ?? true);

  toggleList(): void {
    this.listToggled.set(!this.listOpen());
  }

  readonly LIST_SHOWN = LIST_SHOWN;

  single = computed(() => this.views()[0] ?? null);

  primary = computed<AiRecordPrimary>(() => {
    const single = this.single();
    if (single?.hubItem) {
      return 'install';
    }
    return single && this.made().includes(single.key) ? 'open' : null;
  });

  cardActions = computed<AiCardAction[]>(() => {
    const item = this.single()?.hubItem;
    if (!item || !TARGETED_HUB_TYPES.has(item.type)) {
      return [];
    }
    return [{
      id: 'install-on', icon: 'edit', label: this.translate.instant('ai-assistant.record.install-on'),
      compose: this.translate.instant('ai-assistant.record.install-on-prompt', {name: item.name})
    }];
  });

  shownViews = computed(() => this.allShown() ? this.views() : this.views().slice(0, LIST_SHOWN));

  sameKind = computed(() => new Set(this.views().map(view => this.kindKey(view))).size === 1);

  listKind = computed(() => this.sameKind() && this.views().length ? aiEntityKind(this.views()[0].entityType, this.views()[0].alarmRule) : null);

  listLabel = computed(() => {
    const views = this.views();
    if (views.every(view => !!view.hub)) {
      return this.translate.instant('ai-assistant.record.hub-items', {count: views.length});
    }
    if (!this.sameKind()) {
      return this.translate.instant('ai-assistant.summary.entities', {count: views.length});
    }
    const view = views[0];
    const plural = view.alarmRule ? 'ai-assistant.summary.alarm-rules'
      : view.entityType === 'GATEWAY' ? 'ai-assistant.record.gateways' : entityTypeTranslations.get(view.entityType as EntityType)?.typePlural;
    return plural ? `${views.length} ${this.translate.instant(plural).toLowerCase()}` : this.translate.instant('ai-assistant.summary.entities', {count: views.length});
  });

  private loader = inject(AiRecordLoaderService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);
  private loading: Subscription;

  private key = computed(() => this.shown().refs.map(aiRecordKey).join(','));

  constructor() {
    effect(() => {
      this.key();
      const shown = untracked(() => this.shown());
      this.loading?.unsubscribe();
      this.loading = this.loader.loadAll(shown.refs, {image: shown.refs.length === 1})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(views => this.views.set(views));
    });
  }

  lineMeta(view: AiRecordView): string {
    if (view.hub) {
      return view.meta;
    }
    const parts = view.meta.split(' · ');
    return parts.length > 1 ? parts.slice(1).join(' · ') : '';
  }

  private kindKey(view: AiRecordView): string {
    return view.hub ? 'HUB:' + view.entityType + view.alarmRule : view.alarmRule ? 'ALARM_RULE' : view.entityType;
  }
}
