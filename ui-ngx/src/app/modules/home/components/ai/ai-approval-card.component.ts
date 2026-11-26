// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewChecked,
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
import { ClipboardService } from 'ngx-clipboard';
import { IntegrationType, integrationTypeInfoMap } from '@shared/models/integration.models';
import { ConverterType, converterTypeTranslationMap } from '@shared/models/converter.models';
import { decorateCodeBlocks } from '@home/components/ai/ai-code-block';
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
import { MailTemplate, mailTemplateTranslations } from '@shared/models/settings.models';

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
  image?: boolean;
  currentImage?: boolean;
  code?: boolean;
  lang?: string;
  chip?: {icon?: string; image?: string};
}

const MAX_FIELDS = 5;

const DETAIL_TOKENS = new Set(['server-attributes', 'shared-attributes']);

const VALUE_TOKENS: Record<string, string[]> = {
  undo: ['none'],
  defaultDashboard: ['none'],
  homeDashboard: ['none'],
  clears: ['yes', 'no'],
  edgeTemplateRoot: ['yes'],
  autoAssign: ['yes', 'no'],
  removeVolumes: ['yes', 'no'],
  default: ['yes'],
  operation: ['restart', 'update', 'upgrade', 'delete', 'cancel', 'upgrade agent']
};

const CODE_FIELDS = new Set(['arguments', 'expression', 'output']);

const REVIEW_VERBS = new Set(['install', 'restart', 'update', 'upgrade', 'upgrade-agent', 'delete', 'cancel', 'sync']);

const IMAGE_FIELDS = new Set(['widget-image', 'scada-symbol']);

const BLOCK_FIELDS = new Set(['script']);

const DETAILED_TOOLS = new Set(['save_converter']);

const JSON_BLOCK_AT = 80;

function prettyJson(value: string | undefined | null): string | null {
  const trimmed = value?.trim() ?? '';
  if (trimmed.length < JSON_BLOCK_AT || !/^[[{]/.test(trimmed)) {
    return null;
  }
  try {
    const parsed = JSON.parse(trimmed);
    return parsed && typeof parsed === 'object' ? JSON.stringify(parsed, null, 2) : null;
  } catch {
    return null;
  }
}

const DIFF_CONTEXT = 2;

function withoutTs(text: string): string {
  try {
    const parsed = JSON.parse(text);
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed) && 'ts' in parsed) {
      const rest = {...parsed};
      delete rest.ts;
      return JSON.stringify(rest, null, 2);
    }
  } catch {
  }
  return text;
}

function lineDiff(before: string, after: string): string {
  const a = before.split('\n');
  const b = after.split('\n');
  const table: number[][] = Array.from({length: a.length + 1}, () => new Array(b.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      table[i][j] = a[i] === b[j] ? table[i + 1][j + 1] + 1 : Math.max(table[i + 1][j], table[i][j + 1]);
    }
  }
  const rows: {mark: ' ' | '-' | '+'; text: string}[] = [];
  let i = 0;
  let j = 0;
  while (i < a.length || j < b.length) {
    if (i < a.length && j < b.length && a[i] === b[j]) {
      rows.push({mark: ' ', text: a[i++]});
      j++;
    } else if (i < a.length && (j >= b.length || table[i + 1][j] >= table[i][j + 1])) {
      rows.push({mark: '-', text: a[i++]});
    } else {
      rows.push({mark: '+', text: b[j++]});
    }
  }
  const keep = rows.map((row, index) => row.mark !== ' '
    || rows.slice(Math.max(0, index - DIFF_CONTEXT), index + DIFF_CONTEXT + 1).some(near => near.mark !== ' '));
  const out: string[] = [];
  rows.forEach((row, index) => {
    if (keep[index]) {
      out.push(row.mark + ' ' + row.text);
    } else if (keep[index - 1] || index === 0) {
      out.push('  …');
    }
  });
  return out.join('\n');
}

const isBlock = (field: AiReviewField): boolean => BLOCK_FIELDS.has(field.id) || !!prettyJson(field.value) || !!prettyJson(field.current);

const CONVERTER_ICONS: Record<string, string> = {UPLINK: 'call_received', DOWNLINK: 'call_made'};

const IMAGE_VALUE = /^(tb-image;|\/api\/images\/|data:image\/)/;

const STACK_AT = 24;
const OWN_TYPE_LABELS = new Set(['DASHBOARD_WIDGET', 'DASHBOARD_STATE', 'DASHBOARD_ENTITY_ALIAS', 'DASHBOARD_FILTER', 'NOTIFICATION', 'NOTIFICATION_SETTINGS', 'NOTIFICATION_PREFERENCES', 'IMAGE', 'PLATFORM_SETTINGS', 'RELATION', 'VERSION', 'WHITE_LABELING', 'MAIL_TEMPLATE', 'CUSTOM_TRANSLATION', 'CUSTOM_MENU', 'MOBILE_APP', 'MOBILE_BUNDLE', 'MOBILE_QR_CODE', 'TWO_FACTOR_SETTINGS', 'MY_PROFILE', 'MY_TWO_FACTOR', 'MAIL_SERVER', 'REPORT_COMPONENT', 'REPORT_ENTITY_ALIAS', 'REPORT_FILTER', 'SCHEDULED_REPORT', 'SCADA_SYMBOL']);

const PROFILE_TYPES = new Set<string>([EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE]);

@Component({
  selector: 'tb-ai-approval-card',
  templateUrl: './ai-approval-card.component.html',
  styleUrls: ['./ai-approval-card.component.scss'],
  standalone: false
})
export class AiApprovalCardComponent implements OnChanges, AfterViewChecked {

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
  private readonly clipboard = inject(ClipboardService);
  private readonly host = inject(ElementRef<HTMLElement>);

  private readonly loader = inject(AiRecordLoaderService);

  listing: AiRecordView | null = null;

  private listingId: string | null = null;

  ngAfterViewChecked(): void {
    decorateCodeBlocks(this.host.nativeElement, this.translate, text => this.clipboard.copyFromContent(text));
  }

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
        this.handedFacts = DETAILED_TOOLS.has(this.data.tool ?? '') ? [] : this.reviewFacts();
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
    if (this.review?.verb && REVIEW_VERBS.has(this.review.verb)) {
      return 'ai-assistant.review.verb.' + this.review.verb;
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

  get recordName(): string {
    const name = this.review?.name;
    const template = this.review?.entityType === 'MAIL_TEMPLATE' && name ? mailTemplateTranslations.get(name as MailTemplate) : undefined;
    return template ? this.translate.instant(template) : name;
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
      const integration = review.entityType === 'INTEGRATION' ? integrationTypeInfoMap.get(review.detail as IntegrationType) : undefined;
      parts.push(DETAIL_TOKENS.has(review.detail) ? this.translate.instant('ai-assistant.review.detail.' + review.detail)
        : integration ? this.translate.instant(integration.name) : review.detail);
    }
    if (review.target) {
      parts.push(this.translate.instant(PROFILE_TYPES.has(review.entityType) ? 'ai-assistant.record.on-profile' : 'ai-assistant.record.on',
        {name: review.target}));
    }
    return parts.join(' · ');
  }

  get facts(): AiReviewRow[] {
    const rows = [...this.rows('FACT', field => !isBlock(field)).slice(0, MAX_FIELDS),
      ...this.rows('CHANGE', field => !isBlock(field) && (field.current === undefined || field.current === null))];
    if (this.listing && this.review?.target) {
      rows.push({label: this.translate.instant('ai-assistant.record.set-up-on'), labelCode: false, valueCode: false, value: this.review.target});
    }
    return rows;
  }

  get codeBlocks(): AiReviewRow[] {
    return [...this.rows('FACT', field => isBlock(field)),
      ...this.rows('CHANGE', field => isBlock(field)).map(row => row.current === undefined ? row
        : {...row, value: row.lang === 'json' ? lineDiff(withoutTs(row.current), withoutTs(row.value)) : lineDiff(row.current, row.value),
          current: undefined, lang: 'diff'})];
  }

  private reviewFacts(): AiDetailFact[] {
    return [
      ...this.rows('FACT', field => !isBlock(field)).map(row => row.image ? {label: row.label, raw: true, images: {value: row.value}}
        : {label: row.label, raw: true, value: row.value, mono: row.valueCode, chip: row.chip}),
      ...this.rows('CHANGE', field => !isBlock(field)).map(row => row.image || row.currentImage
        ? {label: row.label, raw: true, images: {current: row.currentImage ? row.current : undefined, value: row.image ? row.value : undefined}}
        : {label: row.label, raw: true, value: `${row.current ?? '—'} → ${row.value}`, mono: row.valueCode})
    ];
  }

  get stackedDiff(): boolean {
    return this.changes.some(change => !change.image && !change.currentImage
      && ((change.current?.length ?? 0) > STACK_AT || (change.value?.length ?? 0) > STACK_AT));
  }

  get changes(): AiReviewRow[] {
    return this.rows('CHANGE', field => !isBlock(field) && field.current !== undefined && field.current !== null);
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

  private rows(kind: AiReviewField['kind'], include: (field: AiReviewField) => boolean = () => true): AiReviewRow[] {
    return (this.review?.fields ?? []).filter(field => field.kind === kind && include(field)).map(field => ({
      label: field.name ?? this.translate.instant('ai-assistant.review.field.' + field.id),
      labelCode: !!field.name,
      valueCode: CODE_FIELDS.has(field.id) && field.value !== undefined && field.value !== null,
      current: kind !== 'CHANGE' || (isBlock(field) && (field.current === undefined || field.current === null)) ? undefined
        : prettyJson(field.current) ?? this.value(field, field.current),
      value: prettyJson(field.value) ?? this.value(field, field.value),
      image: IMAGE_FIELDS.has(field.id) && IMAGE_VALUE.test(field.value ?? ''),
      currentImage: IMAGE_FIELDS.has(field.id) && IMAGE_VALUE.test(field.current ?? ''),
      code: isBlock(field),
      lang: BLOCK_FIELDS.has(field.id) ? 'tbel' : 'json',
      chip: this.chip(field)
    }));
  }

  private chip(field: AiReviewField): {icon?: string; image?: string} | undefined {
    if (field.id === 'integrationType') {
      const info = integrationTypeInfoMap.get(field.value as IntegrationType);
      return info ? {image: info.icon} : undefined;
    }
    if (field.id === 'type' && converterTypeTranslationMap.has(field.value as ConverterType)) {
      return {icon: CONVERTER_ICONS[field.value]};
    }
    return undefined;
  }

  private value(field: AiReviewField, value: string | undefined): string {
    if (value === undefined || value === null) {
      return this.translate.instant('ai-assistant.review.not-set');
    }
    if (field.id === 'when' && !isNaN(Date.parse(value))) {
      return new Date(value).toLocaleString();
    }
    if (field.id === 'integrationType' && integrationTypeInfoMap.has(value as IntegrationType)) {
      return this.translate.instant(integrationTypeInfoMap.get(value as IntegrationType).name);
    }
    if (field.id === 'type' && converterTypeTranslationMap.has(value as ConverterType)) {
      return this.translate.instant(converterTypeTranslationMap.get(value as ConverterType));
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
