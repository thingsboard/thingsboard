// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  AiApprovalReview,
  AiReviewField,
  ApprovalResult,
  ToolApprovalResult,
  ToolExecutionRequested,
  ToolExecutionResult
} from '@shared/models/ai-chat.models';
import { AiAction } from '@home/components/ai/ai-action.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';

export type ApprovalCardState = 'pending' | 'approved' | 'auto' | 'denied' | 'expired';

import { AiDetailFact } from '@home/components/ai/ai-action-detail.models';
import { AiRecordLoaderService } from '@home/components/ai/ai-record-loader.service';
import { AiRecordView } from '@home/components/ai/ai-record.models';

export interface AiReviewRow {
  label: string;
  labelCode: boolean;
  valueCode: boolean;
  current?: string;
  value: string;
}

const MAX_FIELDS = 5;

const DETAIL_TOKENS = new Set(['server-attributes', 'shared-attributes']);

const VALUE_TOKENS: Record<string, string[]> = {
  undo: ['none'],
  defaultDashboard: ['none'],
  homeDashboard: ['none'],
  clears: ['yes', 'no']
};

const CODE_FIELDS = new Set(['arguments', 'expression', 'output']);

const OWN_TYPE_LABELS = new Set(['DASHBOARD_WIDGET', 'DASHBOARD_STATE', 'DASHBOARD_ENTITY_ALIAS', 'DASHBOARD_FILTER', 'NOTIFICATION', 'NOTIFICATION_SETTINGS', 'NOTIFICATION_PREFERENCES']);

const PROFILE_TYPES = new Set<string>([EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE]);

@Component({
  selector: 'tb-ai-approval-card',
  templateUrl: './ai-approval-card.component.html',
  styleUrls: ['./ai-approval-card.component.scss'],
  standalone: false
})
export class AiApprovalCardComponent implements OnChanges {

  @Input() data: ToolExecutionRequested;

  @Input() completedExecution: ToolExecutionResult | null = null;

  @Input() autoApprove = false;

  @Input() answered = false;

  @ViewChild('denyInput') denyInput: ElementRef<HTMLInputElement>;

  @Output() approvalAction = new EventEmitter<ApprovalResult>();

  @Output() askAgain = new EventEmitter<void>();

  denying = false;

  showUnchanged = false;

  state: ApprovalCardState = 'pending';

  handedOver: AiAction | null = null;

  handedFacts: AiDetailFact[] = [];

  private autoApproved = false;

  private readonly cd = inject(ChangeDetectorRef);
  private readonly translate = inject(TranslateService);

  private readonly loader = inject(AiRecordLoaderService);

  listing: AiRecordView | null = null;

  private listingId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    this.loadListing();
    if ((this.data?.autoApproved || this.autoApprove) && this.state === 'pending' && !this.completedExecution) {
      this.autoApproved = true;
      this.denying = false;
      this.state = 'auto';
    }
    if (this.answered && this.state === 'pending' && !this.completedExecution) {
      this.denying = false;
      this.state = 'denied';
    }
    if (changes.completedExecution && this.completedExecution?.executionId === this.data.executionId) {
      const result = this.completedExecution;
      if (result.approvalStatus === ToolApprovalResult.TIMEOUT) {
        this.state = 'expired';
      } else if (result.approvalStatus === ToolApprovalResult.DENIED) {
        this.state = 'denied';
      } else {
        this.autoApproved = this.autoApproved || !!this.data?.autoApproved;
        this.state = this.autoApproved ? 'auto' : 'approved';
        this.handedOver = {
          executionId: result.executionId,
          tool: this.data.tool ?? '',
          arguments: this.data.arguments,
          result: result.result,
          status: result.executionStatus,
          startedAt: this.data.time ?? Date.now(),
          affectedEntities: result.affectedEntities ?? []
        };
        this.handedFacts = this.reviewFacts();
      }
      this.cd.detectChanges();
    }
  }

  get review(): AiApprovalReview | undefined {
    return this.data.review;
  }

  get isDestructive(): boolean {
    return this.data.destructive;
  }

  get headIcon(): string {
    switch (this.state) {
      case 'approved':
        return 'check_circle';
      case 'auto':
        return 'bolt';
      case 'denied':
        return 'block';
      case 'expired':
        return 'timer_off';
      default:
        return 'rate_review';
    }
  }

  get headText(): string {
    return this.translate.instant('ai-assistant.review.head.' + this.state);
  }

  get outcomeText(): string {
    const result = this.completedExecution;
    const failed = result?.executionStatus !== 'SUCCESS';
    return result?.message || this.translate.instant(failed ? 'ai-assistant.error' : 'ai-assistant.approved');
  }

  get planned(): boolean {
    return this.review?.change === 'CREATE';
  }

  get hubVersionId(): string | null {
    const id = this.data?.tool === 'install_iot_hub_item' ? (this.data.arguments as {versionId?: unknown})?.versionId : null;
    return typeof id === 'string' && id ? id : null;
  }

  get approveLabel(): string {
    if (this.hubVersionId) {
      return 'ai-assistant.record.install';
    }
    if (this.isDestructive) {
      return 'ai-assistant.review.delete';
    }
    switch (this.review?.change) {
      case 'CREATE':
        return 'ai-assistant.review.create';
      case 'UPDATE':
        return 'ai-assistant.review.update';
      default:
        return 'ai-assistant.approve';
    }
  }

  get alarmRule(): boolean {
    return this.review?.detail === 'alarm-rule';
  }

  get meta(): string {
    const review = this.review;
    if (!review) {
      return '';
    }
    const parts = [this.alarmRule ? this.translate.instant('ai-assistant.review.type.ALARM_RULE') : this.typeLabel(review.entityType)];
    if (review.detail && !this.alarmRule) {
      parts.push(DETAIL_TOKENS.has(review.detail) ? this.translate.instant('ai-assistant.review.detail.' + review.detail) : review.detail);
    }
    if (review.target) {
      parts.push(this.translate.instant(PROFILE_TYPES.has(review.entityType) ? 'ai-assistant.record.on-profile' : 'ai-assistant.record.on',
        {name: review.target}));
    }
    return parts.join(' · ');
  }

  get facts(): AiReviewRow[] {
    const rows = this.rows('FACT').slice(0, MAX_FIELDS);
    if (this.listing && this.review?.target) {
      rows.push({label: this.translate.instant('ai-assistant.record.set-up-on'), labelCode: false, valueCode: false, value: this.review.target});
    }
    return rows;
  }

  private reviewFacts(): AiDetailFact[] {
    return [
      ...this.rows('FACT').map(row => ({label: row.label, raw: true, value: row.value, mono: row.valueCode})),
      ...this.rows('CHANGE').map(row => ({label: row.label, raw: true, value: `${row.current ?? '—'} → ${row.value}`, mono: row.valueCode}))
    ];
  }

  get changes(): AiReviewRow[] {
    return this.rows('CHANGE');
  }

  get unchanged(): AiReviewRow[] {
    return this.rows('UNCHANGED');
  }

  get recordClosed(): boolean {
    return this.state === 'pending' || this.state === 'denied' || this.state === 'expired';
  }

  approve(): void {
    this.state = this.autoApproved ? 'auto' : 'approved';
    this.denying = false;
    this.approvalAction.emit({executionId: this.data.executionId, approved: true});
  }

  deny(): void {
    if (this.denying) {
      this.sendDeny(this.denyInput?.nativeElement.value ?? '');
      return;
    }
    this.denying = true;
    setTimeout(() => this.denyInput?.nativeElement.focus());
  }

  sendDeny(words: string): void {
    const reason = words.trim();
    this.denying = false;
    this.state = 'denied';
    this.approvalAction.emit(reason
      ? {executionId: this.data.executionId, approved: false, reason}
      : {executionId: this.data.executionId, approved: false});
  }

  closeDeny(): void {
    this.denying = false;
  }

  private loadListing(): void {
    const id = this.hubVersionId;
    if (!id || id === this.listingId) {
      return;
    }
    this.listingId = id;
    this.loader.hub(id).subscribe({
      next: listing => {
        this.listing = listing;
        this.cd.markForCheck();
      },
      error: () => this.listing = null
    });
  }

  private rows(kind: AiReviewField['kind']): AiReviewRow[] {
    return (this.review?.fields ?? []).filter(field => field.kind === kind).map(field => ({
      label: field.name ?? this.translate.instant('ai-assistant.review.field.' + field.id),
      labelCode: !!field.name,
      valueCode: CODE_FIELDS.has(field.id) && field.value !== undefined && field.value !== null,
      current: kind === 'CHANGE' ? this.value(field, field.current) : undefined,
      value: this.value(field, field.value)
    }));
  }

  private value(field: AiReviewField, value: string | undefined): string {
    if (value === undefined || value === null) {
      return this.translate.instant('ai-assistant.review.not-set');
    }
    if (field.id === 'when' && !isNaN(Date.parse(value))) {
      return new Date(value).toLocaleString();
    }
    if (field.id && VALUE_TOKENS[field.id]?.includes(value)) {
      return this.translate.instant(`ai-assistant.review.value.${field.id}-${value}`);
    }
    return value;
  }

  private typeLabel(entityType: string): string {
    if (OWN_TYPE_LABELS.has(entityType)) {
      return this.translate.instant('ai-assistant.review.type.' + entityType);
    }
    const translation = entityTypeTranslations.get(entityType as EntityType)?.type;
    return translation ? this.translate.instant(translation) : entityType;
  }
}
