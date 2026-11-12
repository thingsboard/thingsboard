// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AiRecordView } from '@home/components/ai/ai-record.models';
import { AiRecordLoaderService } from '@home/components/ai/ai-record-loader.service';

@Component({
  selector: 'tb-ai-run-summary',
  template: `
    @if (lead(); as card) {
      <tb-ai-record-card [view]="card" primary="open" [meta]="leadMeta()"></tb-ai-record-card>
    }
    @if (rest().length > 1) {
      <div class="tb-list" [class.open]="listOpen()" [attr.data-pal]="listKind()?.palette ?? null">
        <button type="button" class="tb-list-head" [attr.aria-expanded]="listOpen()" (click)="toggleList()">
          <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ listKind()?.icon ?? 'interests' }}</mat-icon></span>
          <span class="tb-list-label">{{ listLabel() }}</span>
          <mat-icon class="tb-list-chev" aria-hidden="true">expand_more</mat-icon>
        </button>
        @if (listOpen()) {
          <div class="tb-list-body" role="list" [attr.aria-label]="'ai-assistant.summary.made' | translate">
            @for (row of shownRows(); track row.key) {
              <tb-ai-record role="listitem" [entityType]="row.entityType" [alarmRule]="row.alarmRule" [name]="row.name"
                            [meta]="row.meta" [url]="row.url" [state]="row.state" [alarm]="row.alarm"></tb-ai-record>
            }
            @if (rest().length > LIST_SHOWN) {
              <button type="button" class="tb-more" [attr.aria-expanded]="allShown()" (click)="allShown.set(!allShown())">
                <mat-icon class="tb-mat-16" aria-hidden="true">{{ allShown() ? 'expand_less' : 'expand_more' }}</mat-icon>
                {{ allShown() ? ('ai-assistant.summary.fewer' | translate) : ('ai-assistant.summary.more' | translate: {count: rest().length - LIST_SHOWN}) }}
              </button>
            }
          </div>
        }
      </div>
    } @else if (rest().length) {
      <div class="k-recs" role="list" [attr.aria-label]="'ai-assistant.summary.made' | translate">
        @for (row of rest(); track row.key) {
          <tb-ai-record role="listitem" [entityType]="row.entityType" [alarmRule]="row.alarmRule" [name]="row.name"
                        [meta]="row.meta" [url]="row.url" [state]="row.state" [alarm]="row.alarm"></tb-ai-record>
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

  rows = signal<AiRecordView[]>([]);

  lead = computed(() => this.rows().find(row => row.entityType === EntityType.DASHBOARD && !!row.url) ?? null);

  rest = computed(() => this.rows().filter(row => row !== this.lead()));

  readonly LIST_SHOWN = 8;

  private listToggled = signal<boolean | null>(null);

  allShown = signal(false);

  listOpen = computed(() => this.listToggled() ?? this.rest().length <= 3);

  shownRows = computed(() => this.allShown() ? this.rest() : this.rest().slice(0, this.LIST_SHOWN));

  listKind = computed(() => {
    const kinds = new Set(this.rest().map(row => row.alarmRule ? 'ALARM_RULE' : row.entityType));
    return kinds.size === 1 ? aiEntityKind([...kinds][0]) : null;
  });

  listLabel = computed(() => {
    const rows = this.rest();
    const kinds = new Set(rows.map(row => row.alarmRule ? 'ALARM_RULE' : row.entityType));
    if (kinds.size !== 1) {
      return this.translate.instant('ai-assistant.summary.entities', {count: rows.length});
    }
    const kind = [...kinds][0];
    const plural = kind === 'ALARM_RULE' ? 'ai-assistant.summary.alarm-rules' : entityTypeTranslations.get(kind as EntityType)?.typePlural;
    return plural ? `${rows.length} ${this.translate.instant(plural).toLowerCase()}` : this.translate.instant('ai-assistant.summary.entities', {count: rows.length});
  });

  toggleList(): void {
    this.listToggled.set(!this.listOpen());
  }

  private key = computed(() => this.entities().map(entity => `${entity.entityType}:${entity.id}`).join(','));

  private loader = inject(AiRecordLoaderService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  private loading: Subscription;

  constructor() {
    effect(() => {
      this.key();
      const entities = untracked(() => this.entities());
      this.loading?.unsubscribe();
      this.loading = this.loader.loadAll(entities.map(entity => ({entityType: entity.entityType, id: entity.id})))
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(rows => this.rows.set(rows));
    });
  }
}
