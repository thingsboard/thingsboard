// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { ApprovalChatMessage, ChatMessage } from '@shared/models/solution-creator.models';
import { ClipboardService } from 'ngx-clipboard';
import { ApprovalResult, ToolExecutionRequested, ToolExecutionResult } from '@shared/models/ai-chat.models';
import { AiWorkState } from '@home/components/ai/ai-action.models';

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
  @Output() resourceError = new EventEmitter<any>();

  waitApproval = false;

  actionRunning = false;

  private static readonly PIN_TO_BOTTOM_MS = 1500;
  private static readonly COPIED_MS = 1200;

  private releaseBottom: () => void;

  lastAnswerIndex = -1;

  copiedIndex = -1;

  private copiedTimeout: ReturnType<typeof setTimeout>;

  constructor(private elementRef: ElementRef<HTMLElement>,
              private clipboard: ClipboardService) {}

  copyAnswer(answer: HTMLElement, index: number): void {
    const text = (answer.querySelector<HTMLElement>('.agent-body') ?? answer).innerText.trim();
    if (this.clipboard.copyFromContent(text)) {
      this.copiedIndex = index;
      clearTimeout(this.copiedTimeout);
      this.copiedTimeout = setTimeout(() => this.copiedIndex = -1, AiChatMessagesComponent.COPIED_MS);
    }
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
      this.lastAnswerIndex = this.findLastAnswerIndex();
      this.actionRunning = this.messages.some(message => message.from === 'ACTION' && message.data.status === 'RUNNING');
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
    el.scrollTo({top: el.scrollHeight, behavior});
  }

  scrollToLastUserMessage(): void {
    const userMessages = this.elementRef.nativeElement.querySelectorAll('.user');
    if (userMessages.length) {
      userMessages[userMessages.length - 1].scrollIntoView({block: 'start', behavior: 'smooth'});
    }
  }

  private scrollToLastAiMessage(): void {
    const aiMessages = this.elementRef.nativeElement.querySelectorAll('.ai');
    if (aiMessages.length) {
      aiMessages[aiMessages.length - 1].scrollIntoView({block: 'start', behavior: 'smooth'});
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
