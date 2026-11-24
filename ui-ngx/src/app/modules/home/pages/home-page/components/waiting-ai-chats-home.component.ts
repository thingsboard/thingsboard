// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AiChatsService } from '@core/services/ai-chats.service';

const SHOWN = 2;

@Component({
  selector: 'tb-waiting-ai-chats-home',
  template: `
    @if (!chats.statusResolved()) {
      @if (reserved) {
        <div class="tb-ai-attn-block reserve" aria-hidden="true">
          <div class="tb-ai-attn-h">{{ 'home.ai.waiting' | translate }}</div>
          <div class="tb-ai-attn">
            @for (n of reservedPills; track n) {
              <span class="tb-chat-pill placeholder"><span class="tb-chat-pill-skeleton"></span></span>
            }
          </div>
        </div>
      }
    } @else if (waiting().length) {
      <div class="tb-ai-attn-block" [class.appear]="!reserved">
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

  private readonly shownKey = `tb-home-waiting-chats:${getCurrentAuthUser(inject(Store<AppState>))?.userId}`;

  readonly reserved = this.readShown();

  readonly reservedPills = Array.from({length: this.reserved}, (_, i) => i);

  constructor() {
    this.chats.watchStatus();
    effect(() => {
      if (this.chats.statusResolved()) {
        this.writeShown(this.shown().length);
      }
    });
  }

  private readShown(): number {
    try {
      return Math.min(SHOWN, Math.max(0, Number(localStorage.getItem(this.shownKey)) || 0));
    } catch {
      return 0;
    }
  }

  private writeShown(count: number): void {
    try {
      localStorage.setItem(this.shownKey, String(count));
    } catch {
      return;
    }
  }
}
