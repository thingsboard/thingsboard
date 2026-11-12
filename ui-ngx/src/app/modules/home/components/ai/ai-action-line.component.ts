// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { catchError, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { AiRecordLoaderService } from '@home/components/ai/ai-record-loader.service';
import { AiRecordView } from '@home/components/ai/ai-record.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { aiActionDetailView, AiDetailFact, AiDetailNames, aiParse } from '@home/components/ai/ai-action-detail.models';
import {
  AiAction,
  aiEntityKind,
  aiActionDuration,
  aiActionWords
} from '@home/components/ai/ai-action.models';

@Component({
  selector: 'tb-ai-action-line',
  templateUrl: './ai-action-line.component.html',
  styleUrls: ['./ai-action-line.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.run]': 'action().status === "RUNNING"',
    '[class.fail]': 'action().status === "FAILURE"',
    '[class.stopped]': 'action().status === "STOPPED"'
  }
})
export class AiActionLineComponent {

  action = input.required<AiAction>();

  private translate = inject(TranslateService);

  private loader = inject(AiRecordLoaderService);

  hubVersionId = computed(() => {
    const action = this.action();
    const id = action.tool === 'install_iot_hub_item' && action.status !== 'RUNNING' ? (action.arguments as {versionId?: unknown})?.versionId : null;
    return typeof id === 'string' && id ? id : null;
  });

  private groupId = computed(() => {
    const affected = this.action().status === 'SUCCESS' ? this.action().affectedEntities ?? [] : [];
    const entity = this.action().tool === 'update_group_members' ? affected.find(item => item.entityType === EntityType.ENTITY_GROUP) : affected[0];
    return entity?.entityType === EntityType.ENTITY_GROUP ? entity.id : null;
  });

  private groupView = toSignal(toObservable(this.groupId).pipe(
    distinctUntilChanged(),
    switchMap(id => id ? this.loader.entity({entityType: EntityType.ENTITY_GROUP, id} as EntityId).pipe(catchError(() => of(null))) : of(null))
  ), {initialValue: null as AiRecordView | null});

  listing = toSignal(toObservable(this.hubVersionId).pipe(
    distinctUntilChanged(),
    switchMap(id => id ? this.loader.hub(id).pipe(catchError(() => of(null))) : of(null))
  ), {initialValue: null as AiRecordView | null});

  words = computed(() => aiActionWords(this.action().tool, this.action().arguments));

  more = input(0);

  outcomeText = input<string>();

  autoApproved = input(false);

  linkEntity = input(true);

  outcomeChip = computed(() => {
    const affected = this.action().affectedEntities ?? [];
    const entity = this.action().tool === 'update_group_members'
      ? affected.find(item => item.entityType === EntityType.ENTITY_GROUP) ?? affected[0]
      : affected[0];
    const text = this.outcome();
    const match = entity && this.action().status === 'SUCCESS' ? /'([^']{1,120})'/.exec(text) : null;
    if (!match) {
      return null;
    }
    const alarmRule = entity.entityType === EntityType.CALCULATED_FIELD && this.isAlarmRule();
    const kind = aiEntityKind(entity.entityType, alarmRule);
    const base = alarmRule ? '/alarms/alarm-rules' : baseDetailsPageByEntityType.get(entity.entityType as EntityType);
    const groupUrl = entity.entityType === EntityType.ENTITY_GROUP ? this.groupView()?.url : undefined;
    return {
      before: text.slice(0, match.index),
      name: match[1],
      after: text.slice(match.index + match[0].length),
      icon: kind.icon,
      palette: kind.palette,
      link: this.linkEntity() ? (groupUrl ?? (base ? `${base}/${entity.id}` : null)) : null
    };
  });

  private isAlarmRule(): boolean {
    const action = this.action();
    if (action.tool === 'save_alarm_rule') {
      return true;
    }
    const result = aiParse(action.result) as {type?: unknown} | null;
    return action.tool === 'install_iot_hub_item' && result?.type === 'ALARM_RULE';
  }

  private runWords = computed(() => this.translate.instant(this.words().key + '.run', this.words().params));

  running = computed(() => this.more() > 0
    ? this.translate.instant('ai-assistant.work.more', {words: this.runWords(), count: this.more()})
    : this.runWords());

  outcome = computed(() => {
    const words = this.outcomeWords();
    return this.autoApproved() ? this.translate.instant('ai-assistant.action.auto-approved', {words}) : words;
  });

  private outcomeWords(): string {
    const action = this.action();
    if (this.outcomeText()) {
      return this.outcomeText();
    }
    if (action.status === 'FAILURE') {
      return this.reason(action.result) ?? this.translate.instant('ai-assistant.action.failed', {words: this.lowerFirst(this.runWords())});
    }
    const words = this.words();
    return this.translate.instant(words.key + '.done', words.params);
  }

  duration = computed(() => this.action().durationMs != null ? aiActionDuration(this.action().durationMs) : '');

  names = input<AiDetailNames>(new Map());

  facts = input<AiDetailFact[]>([]);

  detail = computed(() => {
    const view = aiActionDetailView(this.action(), this.names());
    const facts = this.facts();
    return facts.length ? {...view, blocks: [...view.blocks, {type: 'facts' as const, rows: facts}]} : view;
  });

  records = computed(() => this.action().status === 'SUCCESS' ? this.action().affectedEntities ?? [] : []);

  hasDetails = computed(() => !!this.detail().hood || !!this.detail().blocks.length || !!this.records().length);

  private reason(result?: string): string | undefined {
    const text = result?.trim();
    if (!text || text.startsWith('{') || text.startsWith('[')) {
      return undefined;
    }
    const line = text.split('\n')[0].trim();
    return line.length > 300 ? line.slice(0, 300) + '…' : line;
  }

  private lowerFirst(words: string): string {
    return words ? words.charAt(0).toLocaleLowerCase() + words.slice(1) : words;
  }
}
