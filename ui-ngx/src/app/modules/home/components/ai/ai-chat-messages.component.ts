// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { ApprovalChatMessage, ChatMessage } from '@shared/models/solution-creator.models';
import { ClipboardService } from 'ngx-clipboard';
import { TranslateService } from '@ngx-translate/core';
import { ApprovalResult, ToolExecutionRequested, ToolExecutionResult } from '@shared/models/ai-chat.models';
import { AiWorkState } from '@home/components/ai/ai-action.models';
import { EntityId } from '@shared/models/id/entity-id';
import { decorateCodeBlocks } from '@home/components/ai/ai-code-block';

@Component({
  selector: 'tb-ai-chat-messages',
  templateUrl: './ai-chat-messages.component.html',
  styleUrls: ['./ai-chat-messages.component.scss'],
  standalone: false
})
export class AiChatMessagesComponent implements OnChanges, OnDestroy {

  @Input() messages: ChatMessage[] = [];
  @Input() loading = false;
  @Input() loadingLongTime = false;
  @Input() additionalStyles: string[];
  @Input() completedExecutions: Record<string, ToolExecutionResult> = {};
  @Input() autoApprove = false;
  @Input() scrollContainer: HTMLElement;
  @Input() initialScroll: 'lastUserMessage' | 'bottom' = 'lastUserMessage';
  @Input() denyReasons: Record<string, string> = {};
  @Input() work: AiWorkState | undefined;
  @Output() approvalAction = new EventEmitter<ApprovalResult>();
  @Output() askAgain = new EventEmitter<void>();
  @Output() editMessage = new EventEmitter<string>();
  @Output() resourceError = new EventEmitter<any>();

  waitApproval = false;

  actionRunning = false;

  runningLead = -1;

  runningCount = 0;

  summaries: Record<number, EntityId[]> = {};

  dayLines: Record<number, string> = {};

  private unfolded = new Set<number>();

  private dayTimer: ReturnType<typeof setTimeout>;

  private static readonly PIN_TO_BOTTOM_MS = 1500;
  private static readonly COPIED_MS = 1200;
  private static readonly FOLD_LINES = 6;
  private static readonly FOLD_CHARS = 600;

  private releaseBottom: () => void;

  lastAnswerIndex = -1;

  copiedIndex = -1;

  private copiedTimeout: ReturnType<typeof setTimeout>;

  constructor(private elementRef: ElementRef<HTMLElement>,
              private clipboard: ClipboardService,
              private translate: TranslateService) {}

  copyText(text: string, index: number): void {
    if (this.clipboard.copyFromContent(text)) {
      this.copiedIndex = index;
      clearTimeout(this.copiedTimeout);
      this.copiedTimeout = setTimeout(() => this.copiedIndex = -1, AiChatMessagesComponent.COPIED_MS);
    }
  }

  decorateCode(answer: HTMLElement): void {
    decorateCodeBlocks(answer, this.translate, text => this.clipboard.copyFromContent(text));
  }

  isFolded(index: number, content: string): boolean {
    return !this.unfolded.has(index) && (content.split('\n').length > AiChatMessagesComponent.FOLD_LINES || content.length > AiChatMessagesComponent.FOLD_CHARS);
  }

  unfold(index: number): void {
    this.unfolded.add(index);
  }

  copyAnswer(answer: HTMLElement, index: number): void {
    const text = (answer.querySelector<HTMLElement>('tb-markdown') ?? answer).innerText.trim();
    if (this.clipboard.copyFromContent(text)) {
      this.copiedIndex = index;
      clearTimeout(this.copiedTimeout);
      this.copiedTimeout = setTimeout(() => this.copiedIndex = -1, AiChatMessagesComponent.COPIED_MS);
    }
  }

  fullTime(time: number): string {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const text = new Intl.DateTimeFormat(undefined, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
    }).format(time);
    return zone ? `${text} (${zone})` : text;
  }

  getApprovalData(msg: ApprovalChatMessage): ToolExecutionRequested {
    return msg.data;
  }

  denyReasonOf(msg: ApprovalChatMessage): string | undefined {
    return this.completedExecutions[msg.data.executionId]?.reason ?? this.denyReasons[msg.data.executionId];
  }

  getCompletedExecution(msg: ApprovalChatMessage): ToolExecutionResult | undefined {
    return this.completedExecutions[msg.data.executionId];
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.loadingLongTime?.currentValue) {
      if (this.isNearBottom()) {
        setTimeout(() => this.scrollToBottom('smooth'), 0);
      }
    }
    if (changes.messages) {
      this.refreshDayLines();
    }
    if (changes.messages || changes.completedExecutions || changes.loading) {
      this.summaries = this.findSummaries();
    }
    if (changes.messages) {
      this.lastAnswerIndex = this.findLastAnswerIndex();
      const running = this.messages.map((message, index) => message.from === 'ACTION' && message.data.status === 'RUNNING' ? index : -1)
        .filter(index => index >= 0);
      this.actionRunning = running.length > 0;
      this.runningLead = running.length ? running[0] : -1;
      this.runningCount = running.length;
      if (changes.messages.firstChange) {
        if (this.initialScroll === 'bottom') {
          this.pinToBottom();
        } else {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        }
        return;
      }
      const wasNearBottom = this.isNearBottom();
      const prevMsgs = changes.messages.previousValue as ChatMessage[];
      const prevLastMsg = prevMsgs?.[prevMsgs.length - 1];
      const msgs = changes.messages.currentValue as ChatMessage[];
      const lastMsg = msgs[msgs.length - 1];
      // In auto-approve we are not blocked on the user, so the assistant keeps working: the
      // thinking indicator must stay visible (waitApproval false) and the view must follow the
      // stream of auto-generated cards down to the bottom.
      this.waitApproval = lastMsg?.from === 'APPROVAL' && !this.autoApprove;
      if (this.autoApprove) {
        setTimeout(() => this.scrollToBottom('auto'), 0);
      } else if (lastMsg?.from === 'USER') {
        setTimeout(() => this.scrollToBottom('smooth'), 0);
      } else if (lastMsg?.from === 'AI') {
        if (prevLastMsg?.from === 'USER') {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        } else if (wasNearBottom) {
          setTimeout(() => this.scrollToLastAiMessage(), 0);
        }
      } else if (lastMsg?.from === 'ACTION') {
        if (wasNearBottom) {
          setTimeout(() => this.scrollToBottom('smooth'), 0);
        }
      } else if (lastMsg?.from === 'APPROVAL') {
        if (prevLastMsg?.from === 'USER') {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        } else if (wasNearBottom) {
          setTimeout(() => this.scrollToLastAiMessage(), 0);
        }
      }
    }
  }

  approval($event: ApprovalResult) {
    this.waitApproval = false;
    this.approvalAction.emit($event);
  }

  private refreshDayLines(): void {
    const lines: Record<number, string> = {};
    let previous: string | undefined;
    let first = true;
    this.messages.forEach((message, index) => {
      const time = (message as {time?: number}).time;
      if (!time) {
        return;
      }
      const day = new Date(time).toDateString();
      if (!first && day !== previous) {
        lines[index] = this.dayLabel(time);
      }
      previous = day;
      first = false;
    });
    this.dayLines = lines;
    clearTimeout(this.dayTimer);
    const midnight = new Date();
    midnight.setHours(24, 0, 1, 0);
    this.dayTimer = setTimeout(() => this.refreshDayLines(), midnight.getTime() - Date.now());
  }

  private dayLabel(time: number): string {
    const date = new Date(time);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const day = new Date(date);
    day.setHours(0, 0, 0, 0);
    const days = Math.round((today.getTime() - day.getTime()) / 86_400_000);
    if (days === 0) {
      return this.translate.instant('ai-assistant.today');
    }
    if (days === 1) {
      return this.translate.instant('ai-assistant.yesterday');
    }
    const options: Intl.DateTimeFormatOptions = days > 1 && days <= 6
      ? {weekday: 'long', day: 'numeric', month: 'long'}
      : date.getFullYear() === today.getFullYear() ? {day: 'numeric', month: 'long'} : {day: 'numeric', month: 'long', year: 'numeric'};
    return new Intl.DateTimeFormat(this.translate.currentLang?.replace('_', '-') || undefined, options).format(date).replace(',', '');
  }

  private findSummaries(): Record<number, EntityId[]> {
    const summaries: Record<number, EntityId[]> = {};
    let entities: EntityId[] = [];
    let seen = new Set<string>();
    let lastAi = -1;
    const close = (finished: boolean) => {
      if (finished && lastAi >= 0 && entities.length) {
        summaries[lastAi] = entities;
      }
      entities = [];
      seen = new Set<string>();
      lastAi = -1;
    };
    const add = (ids?: EntityId[]) => (ids ?? []).forEach(id => {
      const key = `${id.entityType}:${id.id}`;
      if (!seen.has(key)) {
        seen.add(key);
        entities.push(id);
      }
    });
    this.messages.forEach((message, index) => {
      switch (message.from) {
        case 'USER':
          close(true);
          break;
        case 'APPROVAL':
          add((this.completedExecutions[message.data.executionId] ?? message.result)?.affectedEntities);
          break;
        case 'ACTION':
          add(message.data.affectedEntities);
          break;
        case 'AI':
          lastAi = index;
          break;
      }
    });
    close(!this.loading);
    return summaries;
  }

  private findLastAnswerIndex(): number {
    for (let i = this.messages.length - 1; i >= 0; i--) {
      const message = this.messages[i];
      if (message.from === 'AI' && message.status !== 'INCOMPLETE') {
        return i;
      }
    }
    return -1;
  }

  ngOnDestroy(): void {
    clearTimeout(this.copiedTimeout);
    clearTimeout(this.dayTimer);
    this.releaseBottom?.();
  }

  private pinToBottom(): void {
    const scroller = this.scroller;
    const stick = () => scroller.scrollTop = scroller.scrollHeight;
    const observer = new ResizeObserver(stick);
    observer.observe(this.elementRef.nativeElement);
    const release = () => {
      observer.disconnect();
      clearTimeout(timeout);
      scroller.removeEventListener('wheel', release);
      scroller.removeEventListener('touchstart', release);
      scroller.removeEventListener('keydown', release);
      this.releaseBottom = undefined;
    };
    const timeout = setTimeout(release, AiChatMessagesComponent.PIN_TO_BOTTOM_MS);
    scroller.addEventListener('wheel', release, {passive: true});
    scroller.addEventListener('touchstart', release, {passive: true});
    scroller.addEventListener('keydown', release);
    this.releaseBottom = release;
    stick();
  }

  scrollToBottom(behavior: ScrollBehavior = 'smooth'): void {
    const el = this.scroller;
    el.scrollTo({top: el.scrollHeight, behavior: this.motion(behavior)});
  }

  scrollToLastUserMessage(): void {
    const userMessages = this.elementRef.nativeElement.querySelectorAll('.user');
    if (userMessages.length) {
      userMessages[userMessages.length - 1].scrollIntoView({block: 'start', behavior: this.motion('smooth')});
    }
  }

  private scrollToLastAiMessage(): void {
    const aiMessages = this.elementRef.nativeElement.querySelectorAll('.ai');
    if (aiMessages.length) {
      aiMessages[aiMessages.length - 1].scrollIntoView({block: 'start', behavior: this.motion('smooth')});
    }
  }

  parseResult(msg: string): string {
    if (this.isValidJson(msg)) {
      const parsedMsg = JSON.parse(msg);
      if (Array.isArray(parsedMsg)) {
        const dashboardList = parsedMsg.map((item) => {
          return '\n' +
            '#### 📊️ ' + item.name + '\n' +
            '*Assigned to: **' + item.assignedTo + '***\n\n' +
            item.description + '\n';
        });
        return dashboardList.join('---\n');
      } else {
        return '```json\n' +
          msg +
          '{:copy-code}\n' +
          '```';
      }
    } else {
      return msg;
    }
  }

  private motion(behavior: ScrollBehavior): ScrollBehavior {
    return behavior === 'smooth' && matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : behavior;
  }

  private get scroller(): HTMLElement {
    return this.scrollContainer ?? this.elementRef.nativeElement;
  }

  private isNearBottom(threshold = 150): boolean {
    const el = this.scroller;
    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold;
  }

  private isValidJson(str: string): boolean {
    try {
      const result = JSON.parse(str);
      return (typeof result === 'object' && result !== null);
    } catch (e) {
      return false;
    }
  }
}
