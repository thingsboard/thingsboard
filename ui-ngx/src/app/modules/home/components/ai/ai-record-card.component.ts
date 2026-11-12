// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { aiFactParts, AiRecordFact, AiRecordView } from '@home/components/ai/ai-record.models';
import { AiCardAction } from '@home/components/ai/ai-card-actions.component';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';

const TIGHT_WIDTH = 400;

export type AiRecordPrimary = 'open' | 'install' | null;

export function aiDashboardThumb(cards: number): string {
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

@Component({
  selector: 'tb-ai-record-card',
  template: `
    @let rec = view();
    <div #card class="tb-rec card" [attr.data-pal]="kind().palette"
         [class.with-pic]="picture()" [class.primary]="!!primaryAction()" [class.tight]="tight()" [class.with-details]="hasFoot()">
      <div class="tb-rec-row">
        @if (picture()) {
          <span class="tb-rec-pic" [class.sq]="rec.square" [class.wide]="rec.wide" aria-hidden="true">
            @if (rec.image) {
              <img [src]="rec.image | image: {ignoreLoadingImage: true} | async" alt="">
            } @else if (rec.imageUrl) {
              <img [src]="rec.imageUrl" alt="">
            } @else {
              <span class="tb-rec-img" [innerHTML]="thumb()"></span>
            }
            <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ kind().icon }}</mat-icon></span>
          </span>
        } @else {
          <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ kind().icon }}</mat-icon></span>
        }
        <div class="tb-rec-body">
          @if (rec.url) {
            <a class="tb-rec-title" [routerLink]="rec.url">{{ rec.name }}</a>
          } @else {
            <b class="tb-rec-title">{{ rec.name }}</b>
          }
          <span class="tb-rec-meta">
            @if (rec.hub; as listing) {
              <span class="tb-hub-ic tight-only" [matTooltip]="'ai-assistant.detail.on-iot-hub' | translate" matTooltipPosition="above"><mat-icon class="tb-mat-16" aria-hidden="true">hub</mat-icon></span>
              {{ listing.type }}@if (listing.creator) {{{ listing.type ? ' · ' : '' }}{{ listing.creator }}}@if (listing.verified) {<mat-icon class="k-ver" aria-hidden="true">verified</mat-icon>}@if (listing.installs) { · {{ listing.installs }}}
            } @else {
              {{ meta() || rec.meta }}
            }
          </span>
        </div>
        <span class="tb-rec-side">
          @if (rec.hub) {
            <span class="tb-state hub" [matTooltip]="'ai-assistant.detail.on-iot-hub' | translate" matTooltipPosition="above">
              <mat-icon class="tb-mat-16" aria-hidden="true">hub</mat-icon>IoT Hub
            </span>
          } @else if (rec.alarm; as current) {
            <span [class]="'tb-state sev ' + (current.cleared ? 'cleared' : current.severity.toLowerCase())"
                  [matTooltip]="current.tip" matTooltipPosition="above" [attr.aria-label]="current.tip">
              <tb-ai-sev-mark [severity]="current.severity"></tb-ai-sev-mark>{{ (current.cleared ? 'ai-assistant.record.cleared' : 'alarm.severity-' + current.severity.toLowerCase()) | translate }}
              @if (current.acknowledged) {
                <mat-icon class="tb-ack-ic" aria-hidden="true">done_all</mat-icon>
              }
            </span>
          } @else if (rec.state; as current) {
            <span [class]="'tb-state ' + current.tone"><i></i>{{ current.label }}</span>
          }
          @switch (primaryAction()) {
            @case ('install') {
              <button mat-flat-button type="button" class="tb-open-btn" [disabled]="installing()" (click)="install()">
                <mat-icon aria-hidden="true">download</mat-icon>{{ 'ai-assistant.record.install' | translate }}
              </button>
            }
            @case ('open') {
              <a mat-flat-button class="tb-open-btn" [routerLink]="rec.url">
                <mat-icon aria-hidden="true">{{ kind().icon }}</mat-icon>{{ openLabel() }}
              </a>
            }
            @default {
              @if (rec.url) {
                <a class="tb-open" [href]="rec.url" target="_blank" rel="noopener" [matTooltip]="openTip()" matTooltipPosition="above"
                   [attr.aria-label]="openTip() + ': ' + rec.name">
                  <mat-icon class="tb-mat-18" aria-hidden="true">open_in_new</mat-icon>
                </a>
              }
            }
          }
        </span>
      </div>
      @if (hasFoot()) {
        <div class="tb-rec-det">
          @if (note()) {
            <p class="tb-rec-note">{{ note() }}</p>
          }
          @if (facts().length) {
            <dl class="tb-facts tb-rec-facts">
              @for (fact of facts(); track $index) {
                <dt>{{ fact.label }}</dt>
                <dd>@for (part of parts(fact); track $index) {@if (part.code) {<code>{{ part.text }}</code>} @else {{{ part.text }}}}</dd>
              }
            </dl>
          }
          @if (actions().length) {
            <tb-ai-card-actions [actions]="actions()" (act)="act.emit($event)" (compose)="compose.emit($event)"></tb-ai-card-actions>
          }
        </div>
      }
    </div>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiRecordCardComponent {

  view = input.required<AiRecordView>();

  primary = input<AiRecordPrimary>(null);

  meta = input<string | null>(null);

  facts = input<AiRecordFact[]>([]);

  note = input<string | undefined>(undefined);

  actions = input<AiCardAction[]>([]);

  act = output<string>();

  compose = output<string>();

  tight = signal(false);

  installing = signal(false);

  kind = computed(() => aiEntityKind(this.view().entityType, this.view().alarmRule));

  picture = computed(() => {
    const view = this.view();
    return !!(view.image || view.imageUrl || view.entityType === EntityType.DASHBOARD);
  });

  primaryAction = computed<AiRecordPrimary>(() => {
    const primary = this.primary();
    if (primary === 'install') {
      return this.view().hubItem ? 'install' : null;
    }
    return primary === 'open' && this.view().url ? 'open' : null;
  });

  hasFoot = computed(() => !!this.note() || this.facts().length > 0 || this.actions().length > 0);

  thumb = computed(() => this.sanitizer.bypassSecurityTrustHtml(aiDashboardThumb(this.view().widgets ?? 4)));

  openLabel = computed(() => this.translate.instant('ai-assistant.record.open-type', {type: this.typeWord()}));

  openTip = computed(() => this.view().hub ? this.translate.instant('ai-assistant.record.open-on-iot-hub')
    : this.translate.instant('ai-assistant.record.open-new-tab', {type: this.typeWord()}));

  private card = viewChild<ElementRef<HTMLElement>>('card');

  private sanitizer = inject(DomSanitizer);
  private translate = inject(TranslateService);
  private iotHubActions = inject(IotHubActionsService);
  private destroyRef = inject(DestroyRef);

  constructor() {
    effect(onCleanup => {
      const card = this.card()?.nativeElement;
      if (!card || !this.picture() || typeof ResizeObserver === 'undefined') {
        return;
      }
      const observer = new ResizeObserver(() => this.tight.set(card.clientWidth > 0 && card.clientWidth < TIGHT_WIDTH));
      observer.observe(card);
      onCleanup(() => observer.disconnect());
    });
  }

  parts(fact: AiRecordFact) {
    return aiFactParts(fact.value);
  }

  install(): void {
    const item = this.view().hubItem;
    if (!item || this.installing()) {
      return;
    }
    this.installing.set(true);
    this.iotHubActions.installItem(item).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      complete: () => this.installing.set(false),
      error: () => this.installing.set(false)
    });
  }

  private typeWord(): string {
    const view = this.view();
    if (view.alarmRule) {
      return this.translate.instant('ai-assistant.review.type.ALARM_RULE').toLowerCase();
    }
    const key = entityTypeTranslations.get(view.entityType as EntityType)?.type;
    return key ? this.translate.instant(key).toLowerCase() : this.translate.instant('ai-assistant.record.entity');
  }
}
