// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ApprovalChatMessage, ChatMessage } from '@shared/models/solution-creator.models';
import { ApprovalResult, ToolExecutionRequested, ToolExecutionResult } from '@shared/models/ai-chat.models';

@Component({
  selector: 'tb-ai-chat-messages',
  templateUrl: './ai-chat-messages.component.html',
  styleUrls: ['./ai-chat-messages.component.scss'],
  standalone: false
})
export class AiChatMessagesComponent implements OnChanges {

  @Input() messages: ChatMessage[] = [];
  @Input() loading = false;
  @Input() loadingLongTime = false;
  @Input() additionalStyles: string[];
  @Input() completedExecutions: Record<string, ToolExecutionResult> = {};
  @Input() autoApprove = false;
  @Output() approvalAction = new EventEmitter<ApprovalResult>();

  waitApproval = false;

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  getApprovalData(msg: ApprovalChatMessage): ToolExecutionRequested {
    return msg.data;
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
      if (changes.messages.firstChange) {
        setTimeout(() => this.scrollToLastUserMessage(), 0);
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

  scrollToBottom(behavior: ScrollBehavior = 'smooth'): void {
    const el = this.elementRef.nativeElement;
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

  private isNearBottom(threshold = 150): boolean {
    const el = this.elementRef.nativeElement;
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
