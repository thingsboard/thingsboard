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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
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
import { AiSolutionsService } from '@core/services/ai-solutions.service';
import { NEW_SOLUTION_ID, SolutionInfo } from '@shared/models/solution-creator.models';
import { SolutionChatComponent } from '@home/components/ai/solution/solution-chat.component';

export interface AiRailRow {
  id: string;
  title: string;
  createdTime: number;
  pinned: boolean;
  chat?: ChatInfo;
  solution?: SolutionInfo;
}

export interface AiChatsGroup {
  label: string;
  chats: AiRailRow[];
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
  @ViewChild(SolutionChatComponent) solutionChat: SolutionChatComponent;
  @ViewChild('searchInput') searchInput: ElementRef<HTMLInputElement>;

  readonly toastTarget = 'aiAssistantPage';

  readonly newSolutionId = NEW_SOLUTION_ID;

  readonly shortcutModifier = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl ';

  railOpen = signal<boolean>(this.initialRailOpen());

  query = signal<string>('');

  renamingId = signal<string | null>(null);

  chatId = signal<string | undefined>(undefined);

  solutionId = signal<string | undefined>(undefined);

  aiChats = inject(AiChatsService);

  aiSolutions = inject(AiSolutionsService);

  rows = computed<AiRailRow[]>(() => [
    ...this.aiChats.chats().map(chat => ({
      id: chat.id, title: chat.title, createdTime: chat.createdTime, pinned: !!chat.pinned, chat
    })),
    ...this.aiSolutions.solutions().map(solution => ({
      id: solution.id, title: this.aiSolutions.titleOf(solution), createdTime: solution.createdTime ?? 0, pinned: false, solution
    }))
  ].sort((a, b) => b.createdTime - a.createdTime));

  groups = computed<AiChatsGroup[]>(() => {
    const query = this.query().trim().toLowerCase();
    const chats = this.rows();
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
    const needsFirst = (row: AiRailRow) => row.chat && this.aiChats.needsAttention(row.chat) ? 0 : 1;
    groups.forEach(group => group.chats.sort((a, b) => needsFirst(a) - needsFirst(b)));
    return groups.filter(group => group.chats.length);
  });

  waiting = computed<ChatInfo[]>(() => {
    const rank = (chat: ChatInfo) => this.aiChats.levelOf(chat) === 'needs' ? 0 : 1;
    return this.aiChats.chats()
      .filter(chat => this.aiChats.needsAttention(chat))
      .sort((a, b) => rank(a) - rank(b) || b.createdTime - a.createdTime);
  });

  waitItem = computed<{count: number; tip: string; label: string} | null>(() => {
    const waiting = this.waiting();
    if (!waiting.length || (waiting.length === 1 && waiting[0].id === this.chatId())) {
      return null;
    }
    if (waiting.length === 1) {
      const title = waiting[0].title || this.translate.instant('ai-assistant.new-chat');
      return {
        count: 1,
        tip: this.translate.instant('ai-assistant.waiting-one-tip', {title, status: this.aiChats.tipOf(waiting[0])}),
        label: this.translate.instant('ai-assistant.waiting-one', {title})
      };
    }
    const text = this.translate.instant('ai-assistant.waiting-many', {count: waiting.length});
    return {count: waiting.length, tip: text, label: text};
  });

  private translate = inject(TranslateService);
  private store = inject(Store<AppState>);
  private sanitizer = inject(DomSanitizer);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  private browsing = false;

  private pendingEdit: string | null = null;

  private ready = signal(false);

  private urlEffect = effect(() => {
    const chatId = this.chatId() ?? null;
    const solutionId = this.solutionId() ?? null;
    const params = this.route.snapshot.queryParamMap;
    if ((params.get('chatId') !== chatId || params.get('solutionId') !== solutionId) && this.ready()) {
      const browsing = this.browsing;
      this.browsing = false;
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {chatId, solutionId},
        queryParamsHandling: 'merge',
        replaceUrl: !browsing
      });
    }
  });

  private urlSubscription = this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
    if (this.ready()) {
      this.openFromUrl(params.get('chatId'), params.get('solutionId'));
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
    forkJoin([
      this.aiSolutions.load().pipe(catchError(err => {
        this.showErrorMessage(err);
        return of([]);
      })),
      this.aiChats.load()
    ]).subscribe({
      next: ([, chats]) => {
        const params = this.route.snapshot.queryParamMap;
        this.openFromUrl(params.get('chatId'), params.get('solutionId'), chats);
        this.ready.set(true);
      },
      error: err => {
        this.ready.set(true);
        this.showErrorMessage(err);
      }
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
    this.browsing = !!this.chatId() || !!this.solutionId();
    this.solutionId.set(undefined);
    this.chatId.set(undefined);
    setTimeout(() => this.chat?.focus());
  }

  newSolution(): void {
    this.settleRail();
    this.openSolution(NEW_SOLUTION_ID);
  }

  selectRow(row: AiRailRow): void {
    if (row.solution) {
      this.settleRail();
      this.openSolution(row.solution.id);
    } else if (row.chat) {
      this.selectChat(row.chat);
    }
  }

  isActive(row: AiRailRow): boolean {
    return row.solution ? row.id === this.solutionId() : row.id === this.chatId() && !this.solutionId();
  }

  editDesign(solution: SolutionInfo): void {
    if (this.solutionId() === solution.id && this.solutionChat) {
      this.solutionChat.editDesign();
      return;
    }
    this.pendingEdit = solution.id;
    this.openSolution(solution.id);
  }

  onSolutionLoaded(solutionId: string): void {
    if (this.pendingEdit === solutionId) {
      this.pendingEdit = null;
      this.solutionChat?.editDesign();
    }
  }

  uninstallSolution(solution: SolutionInfo): void {
    this.aiSolutions.confirmAndUninstall(solution).subscribe({
      next: info => {
        if (info && this.solutionId() === solution.id) {
          this.solutionChat?.updatedSolution(info);
        }
      },
      error: err => this.showErrorMessage(err)
    });
  }

  deleteSolution(solution: SolutionInfo): void {
    this.aiSolutions.confirmAndDelete(solution).subscribe({
      next: deleted => {
        if (deleted && this.solutionId() === solution.id) {
          this.solutionId.set(undefined);
        }
      },
      error: err => this.showErrorMessage(err)
    });
  }

  private openSolution(solutionId: string): void {
    this.browsing = solutionId !== this.solutionId();
    this.chatId.set(undefined);
    this.solutionId.set(solutionId);
  }

  openWaiting(): void {
    const waiting = this.waiting();
    if (waiting.length === 1) {
      this.selectChat(waiting[0]);
      return;
    }
    this.setRailOpen(true);
    setTimeout(() => this.host.nativeElement.querySelector('.ai-row.attn')?.scrollIntoView({block: 'nearest'}));
  }

  selectChat(chat: ChatInfo): void {
    this.settleRail();
    this.browsing = chat.id !== this.chatId() || !!this.solutionId();
    this.solutionId.set(undefined);
    this.chatId.set(chat.id);
  }

  startRename(chat: {id: string}): void {
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

  finishRename(row: AiRailRow, value: string): void {
    if (this.renamingId() !== row.id) {
      return;
    }
    this.renamingId.set(null);
    const title = value.trim();
    if (!title || title === row.title) {
      return;
    }
    if (row.solution) {
      this.aiSolutions.rename(row.solution, title).subscribe({
        next: info => {
          if (this.solutionId() === row.id) {
            this.solutionChat?.updatedSolution(info);
          }
        },
        error: err => this.showErrorMessage(err)
      });
    } else {
      this.aiChats.rename(row.id, title).subscribe({error: err => this.showErrorMessage(err)});
    }
  }

  togglePinned(chat: ChatInfo): void {
    this.aiChats.setPinned(chat.id, !chat.pinned).subscribe({error: err => this.showErrorMessage(err)});
  }

  deleteChat(chat: ChatInfo): void {
    this.aiChats.confirmAndDelete(chat).subscribe({error: err => this.showErrorMessage(err)});
  }

  private openFromUrl(chatId: string | null, solutionId: string | null, chats = this.aiChats.chats()): void {
    const knownSolution = !solutionId || solutionId === NEW_SOLUTION_ID || !this.aiSolutions.loaded() || !!this.aiSolutions.find(solutionId);
    if (!knownSolution) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {chatId: null, solutionId: NEW_SOLUTION_ID},
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
      return;
    }
    const nextSolution = solutionId || undefined;
    if (nextSolution !== this.solutionId()) {
      this.solutionId.set(nextSolution);
    }
    const known = !nextSolution && !!chatId && chats.some(chat => chat.id === chatId);
    const next = known ? chatId : undefined;
    if (next !== this.chatId()) {
      this.chatId.set(next);
    }
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
