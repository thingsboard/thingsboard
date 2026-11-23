// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { AiChatsService } from '@core/services/ai-chats.service';

const SHOWN = 2;

@Component({
  selector: 'tb-waiting-ai-chats-home',
  template: `
    @if (waiting().length) {
      <div class="tb-ai-attn-block">
        <div class="tb-ai-attn-h" id="tb-ai-attn-h">{{ 'home.ai.waiting' | translate }}</div>
        <div class="tb-ai-attn" role="group" aria-labelledby="tb-ai-attn-h">
          @for (chat of shown(); track chat.id) {
            <a class="tb-chat-pill" routerLink="/ai-assistant" [queryParams]="{chatId: chat.id}" [matTooltip]="chats.tipOf(chat)"
               matTooltipPosition="below">
              <span class="tb-ai-status-mark" [class]="chats.levelOf(chat)" aria-hidden="true"></span>
              <span class="tb-chat-pill-t">{{ chat.title || ('ai-assistant.new-chat' | translate) }}</span>
            </a>
          }
        </div>
      </div>
    }
  `,
  styleUrls: ['./waiting-ai-chats-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WaitingAIChatsHomeComponent {

  readonly chats = inject(AiChatsService);

  readonly waiting = computed(() => this.chats.statusChats().filter(chat => this.chats.needsAttention(chat))
    .sort((a, b) => (this.chats.levelOf(a) === 'error' ? 0 : 1) - (this.chats.levelOf(b) === 'error' ? 0 : 1)));

  readonly shown = computed(() => this.waiting().slice(0, SHOWN));

  constructor() {
    this.chats.watchStatus();
  }
}
