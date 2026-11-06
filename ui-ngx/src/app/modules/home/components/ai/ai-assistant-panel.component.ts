// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, effect, ElementRef, inject, signal, untracked, ViewChild } from '@angular/core';
import { AiAssistantPromptExample } from '@shared/models/ai-chat.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { AiChatsService } from '@core/services/ai-chats.service';
import { AiChatComponent } from '@home/components/ai/ai-chat.component';

@Component({
  selector: 'tb-ai-assistant-panel',
  templateUrl: './ai-assistant-panel.component.html',
  styleUrls: ['./ai-assistant-panel.component.scss'],
  standalone: false,
  host: {
    '[class.open]': 'isOpen',
    '[class.suppressed]': 'panelService.suppressed()',
    '[attr.inert]': 'isOpen ? null : ""',
    '(transitionend)': 'onSlideEnd($event)'
  }
})
export class AiAssistantPanelComponent {

  panelService = inject(AiAssistantPanelService);

  @ViewChild(AiChatComponent) chat: AiChatComponent;

  get isOpen() { return this.panelService.open(); }

  readonly defaultWidth = 364;

  readonly minWidth = 320;

  readonly maxWidth = 480;

  width = signal(this.defaultWidth);

  chatId = signal<string | undefined>(undefined);

  clientContext = computed(() => this.panelService.clientContext() ?? undefined);

  promptExamples = computed<AiAssistantPromptExample[]>(() => this.panelService.config()?.promptExamples ?? []);

  private chats = inject(AiChatsService);
  private elementRef = inject(ElementRef<HTMLElement>);

  private static readonly WIDTH_KEY = 'tbAiPanelWidth';

  private lastContextKey: string | null = null;

  private openEffect = effect(() => {
    if (this.panelService.open()) {
      untracked(() => {
        this.dropChatFromOtherContext();
        this.chats.load().subscribe(() => this.selectLastChat());
      });
    }
  });

  private contextSwitchEffect = effect(() => {
    const key = this.panelService.contextKey();
    if (this.panelService.open() && this.lastContextKey !== null && key !== null && key !== this.lastContextKey) {
      untracked(() => {
        this.chatId.set(undefined);
        this.selectLastChat();
      });
    }
    this.lastContextKey = key;
  });

  constructor() {
    let stored = 0;
    try {
      stored = Number(localStorage.getItem(AiAssistantPanelComponent.WIDTH_KEY)) || 0;
    } catch {
      stored = 0;
    }
    this.setWidth(stored || this.defaultWidth);
  }

  setWidth(width: number, keep = false): void {
    const max = Math.min(this.maxWidth, Math.round(window.innerWidth / 2));
    const value = Math.round(Math.min(max, Math.max(this.minWidth, width)));
    this.width.set(value);
    document.documentElement.style.setProperty('--tb-ai-panel-w', value + 'px');
    if (keep) {
      try {
        if (value === this.defaultWidth) {
          localStorage.removeItem(AiAssistantPanelComponent.WIDTH_KEY);
        } else {
          localStorage.setItem(AiAssistantPanelComponent.WIDTH_KEY, String(value));
        }
      } catch {
        return;
      }
    }
  }

  onGripKey(event: KeyboardEvent): void {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      this.setWidth(this.width() + (event.key === 'ArrowLeft' ? 16 : -16), true);
    }
  }

  startResize(event: PointerEvent): void {
    event.preventDefault();
    const grip = event.target as HTMLElement;
    const startX = event.clientX;
    const startWidth = this.width();
    grip.setPointerCapture(event.pointerId);
    document.body.classList.add('tb-ai-resizing');
    const move = (e: PointerEvent) => this.setWidth(startWidth + (startX - e.clientX));
    const up = () => {
      grip.removeEventListener('pointermove', move);
      grip.removeEventListener('pointerup', up);
      grip.removeEventListener('pointercancel', up);
      document.body.classList.remove('tb-ai-resizing');
      this.setWidth(this.width(), true);
    };
    grip.addEventListener('pointermove', move);
    grip.addEventListener('pointerup', up);
    grip.addEventListener('pointercancel', up);
  }

  close(): void {
    this.panelService.close();
  }

  onChatId(chatId: string | undefined): void {
    this.chatId.set(chatId);
    this.panelService.setLastChatId(chatId);
  }

  onSlideEnd(event: TransitionEvent): void {
    if (event.target !== this.elementRef.nativeElement || event.propertyName !== 'transform' || !this.isOpen) {
      return;
    }
    this.chat?.focus();
  }

  private dropChatFromOtherContext(): void {
    const chatId = this.chatId();
    if (chatId && chatId !== this.panelService.lastChatIdForContext()) {
      this.chatId.set(undefined);
    }
  }

  private selectLastChat(): void {
    if (this.chatId()) {
      return;
    }
    const lastId = this.panelService.lastChatIdForContext();
    if (lastId && this.chats.find(lastId)) {
      this.chatId.set(lastId);
    }
  }
}
