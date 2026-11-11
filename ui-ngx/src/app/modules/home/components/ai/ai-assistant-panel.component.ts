// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AiAssistantViewType, AiChatClientContext } from '@shared/models/ai-chat.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { BreadCrumb } from '@shared/components/breadcrumb';
import { UtilsService } from '@core/services/utils.service';
import { BreadcrumbService } from '@core/services/breadcrumb.service';
import { filter, map, startWith } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, NavigationEnd } from '@angular/router';
import { aiWelcome } from '@home/components/ai/ai-welcome.models';
import { Component, computed, effect, ElementRef, inject, signal, untracked, ViewChild } from '@angular/core';
import { AiAssistantPromptExample } from '@shared/models/ai-chat.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { TranslateService } from '@ngx-translate/core';
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
    '[class.split]': 'split()',
    '(transitionend)': 'onSlideEnd($event)',
    '(window:resize)': 'fit()',
    '(click)': 'onClick($event)'
  }
})
export class AiAssistantPanelComponent {

  panelService = inject(AiAssistantPanelService);

  private translate = inject(TranslateService);

  @ViewChild(AiChatComponent) chat: AiChatComponent;

  get isOpen() { return this.panelService.open(); }

  readonly defaultWidth = 364;

  readonly minWidth = 320;

  readonly maxWidth = 480;

  private static readonly SPLIT_MAX = 880;
  private static readonly SPLIT_MIN = 540;
  private static readonly PAGE_MIN = 560;
  private static readonly SHEET_MAX_WINDOW = 1100;

  width = signal(this.defaultWidth);

  room = signal(0);

  splitWidth = computed(() => {
    const room = this.room();
    return Math.min(AiAssistantPanelComponent.SPLIT_MAX, Math.floor(room / 2), room - AiAssistantPanelComponent.PAGE_MIN);
  });

  splitAvailable = computed(() => this.room() > 0 && window.innerWidth > AiAssistantPanelComponent.SHEET_MAX_WINDOW
    && this.splitWidth() >= AiAssistantPanelComponent.SPLIT_MIN);

  split = computed(() => this.width() > this.maxWidth);

  private wanted = this.defaultWidth;

  private panelWidth = this.defaultWidth;

  chatId = signal<string | undefined>(undefined);

  private router = inject(Router);

  private utils = inject(UtilsService);

  private pageTitle = toSignal(inject(BreadcrumbService).breadcrumbs$.pipe(
    map(crumbs => crumbs.map(crumb => this.crumbLabel(crumb)).filter(label => !!label).join(' › '))
  ), {initialValue: ''});

  private pagePath = toSignal(this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    map(event => event.urlAfterRedirects),
    startWith(this.router.url),
    map(url => (url || '/').split(/[?#]/)[0])
  ), {initialValue: '/'});

  clientContext = computed<AiChatClientContext | undefined>(() => {
    const context = this.panelService.clientContext();
    if (context) {
      return context;
    }
    const title = this.pageTitle();
    const path = this.pagePath();
    return title || path !== '/' ? {view: {type: AiAssistantViewType.PAGE, title, path}, timeZone: getDefaultTimezone()} : undefined;
  });

  promptExamples = computed<AiAssistantPromptExample[]>(() => this.panelService.config()?.promptExamples ?? []);

  welcome = computed(() => aiWelcome(this.panelService.clientContext()?.view, this.panelService.viewName(), this.panelService.config(),
    this.translate));

  private chats = inject(AiChatsService);
  private elementRef = inject(ElementRef<HTMLElement>);

  private static readonly WIDTH_KEY = 'tbAiPanelWidth';

  private lastContextKey: string | null = null;

  private openEffect = effect(() => {
    if (this.panelService.open()) {
      untracked(() => {
        const carried = this.panelService.hasCarriedChat();
        if (!carried) {
          this.dropChatFromOtherContext();
        }
        this.chats.load().subscribe(() => {
          if (carried) {
            const chatId = this.panelService.takeCarriedChat();
            if (chatId && this.chats.find(chatId)) {
              this.chatId.set(chatId);
              return;
            }
          }
          this.selectLastChat();
        });
      });
    }
  });

  private contextSwitchEffect = effect(() => {
    const key = this.panelService.contextKey();
    if (this.panelService.open() && this.lastContextKey !== null && key !== null && key !== this.lastContextKey) {
      untracked(() => {
        const carried = this.panelService.takeCarriedChat();
        if (carried && carried === this.chatId()) {
          return;
        }
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
    this.wanted = stored || this.defaultWidth;
    this.panelWidth = Math.min(this.wanted, this.maxWidth);
    setTimeout(() => this.fit());
  }

  fit(): void {
    this.room.set(this.elementRef.nativeElement.parentElement?.clientWidth ?? window.innerWidth);
    this.show(this.wanted);
  }

  toggleSplit(): void {
    if (this.split()) {
      this.setWidth(this.panelWidth, true);
    } else if (this.splitAvailable()) {
      this.panelWidth = this.width();
      this.setWidth(this.splitWidth(), true);
    }
  }

  private widest(): number {
    return this.splitAvailable() ? this.splitWidth() : Math.min(this.maxWidth, Math.max(this.minWidth, Math.round(this.room() / 2) || this.maxWidth));
  }

  private show(width: number): number {
    const value = Math.round(Math.min(this.widest(), Math.max(this.minWidth, width)));
    this.width.set(value);
    document.documentElement.style.setProperty('--tb-ai-panel-w', value + 'px');
    return value;
  }

  setWidth(width: number, keep = false): void {
    this.room.set(this.elementRef.nativeElement.parentElement?.clientWidth ?? window.innerWidth);
    const value = this.show(width);
    if (keep) {
      this.wanted = value;
      if (value <= this.maxWidth) {
        this.panelWidth = value;
      }
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
    } else if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault();
      this.setWidth(event.key === 'Home' ? this.widest() : this.minWidth, true);
    }
  }

  startResize(event: PointerEvent): void {
    event.preventDefault();
    const grip = event.target as HTMLElement;
    const startX = event.clientX;
    const startWidth = this.width();
    let moved = false;
    grip.setPointerCapture(event.pointerId);
    document.body.classList.add('tb-ai-resizing');
    const move = (e: PointerEvent) => {
      moved = true;
      this.setWidth(startWidth + (startX - e.clientX));
    };
    const up = () => {
      grip.removeEventListener('pointermove', move);
      grip.removeEventListener('pointerup', up);
      grip.removeEventListener('pointercancel', up);
      document.body.classList.remove('tb-ai-resizing');
      if (moved) {
        this.setWidth(this.width(), true);
      }
    };
    grip.addEventListener('pointermove', move);
    grip.addEventListener('pointerup', up);
    grip.addEventListener('pointercancel', up);
  }

  private crumbLabel(crumb: BreadCrumb): string {
    try {
      return crumb.labelFunction ? crumb.labelFunction() : crumb.customTranslate ? this.utils.customTranslation(crumb.label, crumb.label)
        : this.translate.instant(crumb.label);
    } catch {
      return '';
    }
  }

  close(): void {
    this.panelService.close();
  }

  onClick(event: MouseEvent): void {
    const chatId = this.chatId();
    const link = (event.target as HTMLElement | null)?.closest?.('a[href]') as HTMLAnchorElement | null;
    if (!chatId || !link || link.target === '_blank' || link.origin !== window.location.origin
      || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    this.panelService.carryChat(chatId);
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
    const carried = this.panelService.takeCarriedChat();
    if (carried && this.chats.find(carried)) {
      this.chatId.set(carried);
      return;
    }
    const lastId = this.panelService.lastChatIdForContext();
    if (lastId && this.chats.find(lastId)) {
      this.chatId.set(lastId);
    }
  }
}
