// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  computed,
  effect,
  ElementRef,
  HostListener,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AiChatsService } from '@core/services/ai-chats.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { ChatInfo } from '@shared/models/ai-chat.models';
import { DAY, WEEK } from '@shared/models/time/time.models';
import { AiChatComponent } from '@home/components/ai/ai-chat.component';

export interface AiChatsGroup {
  label: string;
  chats: ChatInfo[];
}

@Component({
  selector: 'tb-ai-assistant',
  templateUrl: './ai-assistant.component.html',
  styleUrls: ['./ai-assistant.component.scss'],
  standalone: false,
  host: {
    'data-ai': '',
    '[class.rail-open]': 'railOpen()'
  }
})
export class AiAssistantComponent implements OnInit, OnDestroy {

  private static readonly RAIL_OPEN_KEY = 'tbAiAssistantRailOpen';
  private static readonly NARROW_WIDTH = 900;
  private static readonly REFRESH_MS = 20000;

  @ViewChild(AiChatComponent) chat: AiChatComponent;
  @ViewChild('searchInput') searchInput: ElementRef<HTMLInputElement>;

  readonly toastTarget = 'aiAssistantPage';

  readonly shortcutModifier = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl ';

  railOpen = signal<boolean>(this.initialRailOpen());

  query = signal<string>('');

  renamingId = signal<string | null>(null);

  chatId = signal<string | undefined>(undefined);

  aiChats = inject(AiChatsService);

  groups = computed<AiChatsGroup[]>(() => {
    const query = this.query().trim().toLowerCase();
    const chats = this.aiChats.chats();
    if (query) {
      const found = chats.filter(chat => (chat.title ?? '').toLowerCase().includes(query));
      return found.length
        ? [{label: this.translate.instant('ai-assistant.chats-found', {count: found.length}), chats: found}]
        : [];
    }
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const groups: AiChatsGroup[] = [
      {label: this.translate.instant('ai-assistant.pinned'), chats: []},
      {label: this.translate.instant('ai-assistant.today'), chats: []},
      {label: this.translate.instant('ai-assistant.yesterday'), chats: []},
      {label: this.translate.instant('ai-assistant.last-7-days'), chats: []},
      {label: this.translate.instant('ai-assistant.older'), chats: []}
    ];
    for (const chat of chats) {
      const index = chat.pinned ? 0 : chat.createdTime >= today ? 1 : chat.createdTime >= today - DAY ? 2 : chat.createdTime >= today - WEEK ? 3 : 4;
      groups[index].chats.push(chat);
    }
    const needsFirst = (chat: ChatInfo) => this.aiChats.levelOf(chat) === 'needs' ? 0 : 1;
    groups.forEach(group => group.chats.sort((a, b) => needsFirst(a) - needsFirst(b)));
    return groups.filter(group => group.chats.length);
  });

  private translate = inject(TranslateService);
  private store = inject(Store<AppState>);
  private sanitizer = inject(DomSanitizer);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  private urlEffect = effect(() => {
    const chatId = this.chatId() ?? null;
    if (this.route.snapshot.queryParamMap.get('chatId') !== chatId && this.aiChats.loaded()) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {chatId},
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }
  });

  private refreshTimer = setInterval(() => {
    if (!document.hidden) {
      this.aiChats.refresh().subscribe({error: () => {}});
    }
  }, AiAssistantComponent.REFRESH_MS);

  ngOnDestroy(): void {
    clearInterval(this.refreshTimer);
  }

  ngOnInit(): void {
    this.aiChats.load().subscribe({
      next: chats => {
        const chatId = this.route.snapshot.queryParamMap.get('chatId');
        if (chatId && chats.some(chat => chat.id === chatId)) {
          this.chatId.set(chatId);
        }
      },
      error: err => this.showErrorMessage(err)
    });
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && !event.altKey && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.openSearch();
    }
  }

  toggleRail(): void {
    this.setRailOpen(!this.railOpen());
  }

  openSearch(): void {
    this.setRailOpen(true);
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      if (this.query()) {
        this.query.set('');
      } else {
        (event.target as HTMLInputElement).blur();
      }
    }
  }

  onChatClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.settleRail();
    }
  }

  newChat(): void {
    this.settleRail();
    this.chatId.set(undefined);
    setTimeout(() => this.chat?.focus());
  }

  selectChat(chat: ChatInfo): void {
    this.settleRail();
    this.chatId.set(chat.id);
  }

  startRename(chat: ChatInfo): void {
    this.setRailOpen(true);
    this.renamingId.set(chat.id);
    setTimeout(() => {
      const input = this.host.nativeElement.querySelector<HTMLInputElement>('.ai-rename');
      input?.focus();
      input?.select();
    });
  }

  onRenameKeydown(event: KeyboardEvent): void {
    if (event.isComposing) {
      return;
    }
    if (event.key === 'Enter') {
      (event.target as HTMLInputElement).blur();
    } else if (event.key === 'Escape') {
      this.renamingId.set(null);
    }
  }

  finishRename(chat: ChatInfo, value: string): void {
    if (this.renamingId() !== chat.id) {
      return;
    }
    this.renamingId.set(null);
    const title = value.trim();
    if (title && title !== chat.title) {
      this.aiChats.rename(chat.id, title).subscribe({error: err => this.showErrorMessage(err)});
    }
  }

  togglePinned(chat: ChatInfo): void {
    this.aiChats.setPinned(chat.id, !chat.pinned).subscribe({error: err => this.showErrorMessage(err)});
  }

  deleteChat(chat: ChatInfo): void {
    this.aiChats.confirmAndDelete(chat).subscribe({error: err => this.showErrorMessage(err)});
  }

  private setRailOpen(open: boolean): void {
    this.railOpen.set(open);
    try {
      sessionStorage.setItem(AiAssistantComponent.RAIL_OPEN_KEY, open ? '1' : '0');
    } catch {
      return;
    }
  }

  private settleRail(): void {
    if (this.railOpen() && window.innerWidth < AiAssistantComponent.NARROW_WIDTH) {
      this.setRailOpen(false);
    }
  }

  private initialRailOpen(): boolean {
    try {
      const stored = sessionStorage.getItem(AiAssistantComponent.RAIL_OPEN_KEY);
      return stored === null ? window.innerWidth >= AiAssistantComponent.NARROW_WIDTH : stored === '1';
    } catch {
      return true;
    }
  }

  private showErrorMessage(error: any): void {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    this.store.dispatch(new ActionNotificationShow({
      message: parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message,
      target: this.toastTarget,
      modern: true,
      type: 'error',
      verticalPosition: 'bottom',
      horizontalPosition: 'center'
    }));
  }
}
