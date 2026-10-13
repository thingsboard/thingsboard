// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  OnInit,
  SecurityContext,
  signal,
  ViewChild
} from '@angular/core';
import { ChatMessage } from '@shared/models/solution-creator.models';
import { Observable, of, Subject } from 'rxjs';
import { concatMap, finalize, switchMap, takeUntil, tap } from 'rxjs/operators';
import { AiChatService } from '@core/http/ai-chat.service';
import {
  AiAssistantPromptExample,
  AiChatClientContext,
  ApprovalResult,
  ChatEvent,
  ChatInfo,
  ChatTitleGeneratedEvent,
  SendChatMessageRequest,
  ToolExecutionRequestedEvent,
  ToolExecutionResult
} from '@shared/models/ai-chat.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { AiRenameDialogComponent, AiRenameDialogData } from '@home/components/ai/ai-rename-dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DAY, WEEK } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual, isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { DomSanitizer } from '@angular/platform-browser';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';
import { MatButton } from '@angular/material/button';
import { EntityId } from '@shared/models/id/entity-id';

export interface ChatGroup {
  label: string;
  chats: ChatInfo[];
}

@Component({
  selector: 'tb-ai-assistant-panel',
  templateUrl: './ai-assistant-panel.component.html',
  styleUrls: ['./ai-assistant-panel.component.scss'],
  standalone: false,
  host: {
    '[class.open]': 'isOpen',
    '[attr.inert]': 'isOpen ? null : ""',
    '(transitionend)': 'onSlideEnd($event)'
  }
})
export class AiAssistantPanelComponent implements OnInit {

  panelService = inject(AiAssistantPanelService);

  @ViewChild(AiPromptInputComponent) promptInput: AiPromptInputComponent;

  get isOpen() { return this.panelService.open(); }

  slideDone = signal<boolean>(false);

  loading = signal<boolean>(false);

  isCreatingNew = signal<boolean>(false);

  messages = signal<ChatMessage[]>([]);

  completedExecutions = signal<Record<string, ToolExecutionResult>>({});

  chatId = signal<string | undefined>(undefined);

  subTitle = computed(() => {
    return this.panelService.config()?.subTitle ?? this.translate.instant('ai-assistant.sub-title');
  });

  activePlaceholder = computed(() => {
    const config = this.panelService.config();
    if (this.messages().length) {
      return config?.followUpPromptPlaceholder ?? this.translate.instant('ai-assistant.follow-up-prompt-placeholder');
    }
    return config?.initialPromptPlaceholder ?? '';
  });

  promptExamples = computed<AiAssistantPromptExample[]>(() => this.panelService.config()?.promptExamples ?? []);

  autoApprove = computed(() => this.panelService.isAutoApprove(this.chatId()));

  selectedChatTitle = computed(() => {
    const id = this.chatId();
    if (!id) return null;
    return (this.panelService.chats() ?? []).find(c => c.id === id)?.title ?? null;
  });

  groupedChats = computed<ChatGroup[]>(() => {
    const chats = this.panelService.chats() ?? [];
    if (!chats.length) return [];

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const yesterday = today - DAY;
    const lastWeek = today - WEEK;

    const groups: ChatGroup[] = [
      {label: 'ai-assistant.today', chats: []},
      {label: 'ai-assistant.yesterday', chats: []},
      {label: 'ai-assistant.last-7-days', chats: []},
      {label: 'ai-assistant.older', chats: []}
    ];

    for (const chat of chats) {
      if (chat.createdTime >= today) {
        groups[0].chats.push(chat);
      } else if (chat.createdTime >= yesterday) {
        groups[1].chats.push(chat);
      } else if (chat.createdTime >= lastWeek) {
        groups[2].chats.push(chat);
      } else {
        groups[3].chats.push(chat);
      }
    }

    return groups.filter(g => g.chats.length > 0);
  });

  private aiChat = inject(AiChatService);
  private dialog = inject(DialogService);
  private translate = inject(TranslateService);
  private matDialog = inject(MatDialog);
  private store = inject(Store<AppState>);
  private destroyRef = inject(DestroyRef);
  private sanitizer = inject(DomSanitizer);
  private elementRef = inject(ElementRef<HTMLElement>);

  private loadChat$ = new Subject<string>();
  private cancelMessage$ = new Subject<void>();
  private sessionAffectedEntities: EntityId[] = [];
  private messageCancelled = false;

  private openEffect = effect(() => {
    if (this.panelService.open()) {
      this.dropChatFromOtherContext();
      this.loadChats();
    } else {
      this.slideDone.set(false);
      this.suspendOnClose();
    }
  });

  private lastContextKey: string | null = null;

  // Realign the open chat when the context changes in place while the panel stays open
  // (e.g. switching between dashboards). List/entity toggles collapse to the same key, so they
  // do not trigger this. Guarded on a non-null key to avoid reacting to teardown clearing context.
  private contextSwitchEffect = effect(() => {
    const key = this.panelService.contextKey();
    if (this.panelService.open() && this.lastContextKey !== null && key !== null && key !== this.lastContextKey) {
      this.reset();
      this.loadChats();
    }
    this.lastContextKey = key;
  });

  ngOnInit() {
    this.loadChat$.pipe(
      switchMap(id => {
        return this.aiChat.getChatMessages(id, {ignoreLoading: true, ignoreErrors: true});
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(messages => {
      this.messages.set(messages);
    });
  }

  close(): void {
    this.panelService.close();
  }

  // Credit subscription and focus wait for the slide so they do not stutter it.
  onSlideEnd(event: TransitionEvent): void {
    if (event.target !== this.elementRef.nativeElement || event.propertyName !== 'transform' || !this.isOpen) {
      return;
    }
    this.slideDone.set(true);
    this.promptInput?.focus();
  }

  onApprovalAction(event: ApprovalResult): void {
    const chatId = this.chatId();
    if (event.autoApprove && chatId) {
      this.panelService.setAutoApprove(chatId, true);
    }
    this.aiChat.resolveToolApproval(
      {executionId: event.executionId, approved: event.approved},
      {ignoreLoading: true, ignoreErrors: true}
    ).subscribe();
  }

  setMenuWidth(triggerEl: MatButton): void {
    const width = triggerEl._elementRef.nativeElement.offsetWidth;
    document.documentElement.style.setProperty('--chat-menu-dynamic-width', `${width}px`);
  }

  selectChat(chat: ChatInfo): void {
    this.messageCancelled = true;
    this.cancelMessage$.next();
    this.isCreatingNew.set(false);
    this.chatId.set(chat.id);
    this.panelService.setLastChatId(chat.id);
    this.loadChat$.next(chat.id);
  }

  useExample(example: AiAssistantPromptExample): void {
    this.promptInput?.setValue(example.message);
  }

  sendMsgFromUser(msg: string): void {
    this.messages.update(list => [...list, {from: 'USER', content: msg}]);
    this.sendMessage(msg).subscribe({
      error: (err) => {
        this.messages.update(list => list.slice(0, -1));
        setTimeout(() => {
          this.showErrorMessage(err);
        }, 0);
      },
      complete: () => {
        if (!this.messageCancelled && this.sessionAffectedEntities.length) {
          this.panelService.emitUpdatedData(this.sessionAffectedEntities);
        }
      }
    });
  }

  reset(createNew?: boolean): void {
    this.cancelActiveMessage();
    this.isCreatingNew.set(!!createNew);
    this.chatId.set(undefined);
    this.messages.set([]);
  }

  private cancelActiveMessage(): void {
    this.messageCancelled = true;
    this.cancelMessage$.next();
    this.stopLoadingState();
    this.promptInput?.reset();
  }

  // Keeps the open chat so reopening shows it right away, unless an interrupted reply left it stale.
  private suspendOnClose(): void {
    if (this.loading()) {
      this.reset();
    } else {
      this.cancelActiveMessage();
    }
  }

  private dropChatFromOtherContext(): void {
    const chatId = this.chatId();
    if (chatId && chatId !== this.panelService.lastChatIdForContext()) {
      this.reset();
    }
  }

  renameChat(): void {
    const chatId = this.chatId();
    if (!chatId) return;
    this.matDialog.open<AiRenameDialogComponent, AiRenameDialogData, string>(AiRenameDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: this.translate.instant('ai-assistant.rename-chat-title'),
        value: this.selectedChatTitle() || this.translate.instant('ai-assistant.new-chat')
      }
    }).afterClosed().subscribe(newTitle => {
      if (newTitle && newTitle !== this.selectedChatTitle()) {
        this.aiChat.updatedChat(chatId, {title: newTitle}, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: () => {
            this.panelService.updateChatTitle(chatId, newTitle);
          },
          error: (err) => {
            this.showErrorMessage(err);
          }
        });
      }
    });
  }

  deleteChat(): void {
    const chatId = this.chatId();
    if (!chatId) return;
    const chatTitle = this.selectedChatTitle() || this.translate.instant('ai-assistant.new-chat');
    this.dialog.confirm(
      this.translate.instant('ai-assistant.delete-chat-title', {name: chatTitle}),
      this.translate.instant('ai-assistant.delete-chat-text'),
      this.translate.instant('action.no'),
      this.translate.instant('ai-assistant.delete-chat-confirm')
    ).subscribe(value => {
      if (value) {
        this.aiChat.deleteChat(chatId, {ignoreLoading: true, ignoreErrors: true}).subscribe(() => {
          this.panelService.removeChat(chatId);
          this.reset();
        });
      }
    });
  }

  private loadChats(): void {
    const chats = this.panelService.chats();
    if (chats !== null) {
      this.selectLastChat(chats);
      return;
    }
    this.aiChat.listChats({ignoreLoading: true, ignoreErrors: true}).subscribe(chats => {
      this.panelService.setChats(chats);
      this.selectLastChat(chats);
    });
  }

  private selectLastChat(chats: ChatInfo[]): void {
    if (!this.chatId() && !this.isCreatingNew()) {
      const lastId = this.panelService.lastChatIdForContext();
      const lastChat = lastId ? chats.find(c => c.id === lastId) : null;
      if (lastChat) {
        this.selectChat(lastChat);
      }
    }
  }

  private sendMessage(msg: string): Observable<ChatEvent> {
    this.initLoadingState();
    this.sessionAffectedEntities = [];
    this.messageCancelled = false;

    const startOrContinue$ = this.chatId()
      ? of(null)
      : this.aiChat.createChat({title: 'New chat'}, {ignoreLoading: true, ignoreErrors: true}).pipe(
        tap(chatId => {
          this.isCreatingNew.set(false);
          this.chatId.set(chatId);
          this.panelService.setLastChatId(chatId);
          this.panelService.addChat({id: chatId, title: 'New chat', createdTime: Date.now()});
        })
      );

    const request: SendChatMessageRequest = {
      message: msg,
      clientContext: this.resolveClientContextToSend()
    };

    return startOrContinue$.pipe(
      switchMap(() => this.aiChat.sendChatMessage(this.chatId(), request, {ignoreLoading: true, ignoreErrors: true})),
      tap(() => {
        if (request.clientContext) {
          this.panelService.markClientContextSent(this.chatId(), request.clientContext);
        }
      }),
      takeUntil(this.cancelMessage$),
      concatMap(chatMsg => this.handleChatEvent(chatMsg)),
      finalize(() => this.stopLoadingState())
    );
  }

  private resolveClientContextToSend(): AiChatClientContext | undefined {
    const currentContext = this.panelService.clientContext();
    if (!currentContext) {
      return undefined;
    }
    const chatId = this.chatId();
    const alreadySent = chatId ? this.panelService.getSentClientContext(chatId) : undefined;
    return (!chatId || !isEqual(currentContext, alreadySent)) ? currentContext : undefined;
  }

  private handleChatEvent(chatMsg: ChatEvent): Observable<ChatEvent> {
    switch (chatMsg.event) {
      case 'assistantMessage':
        this.messages.update(list => [...list, {from: 'AI', content: chatMsg.data.message}]);
        return of(chatMsg);
      case 'toolExecutionRequested':
        return this.handleToolApproval(chatMsg);
      case 'toolExecutionResult':
        this.completedExecutions.update(map => ({...map, [chatMsg.data.executionId]: chatMsg.data}));
        if (chatMsg.data.affectedEntities?.length) {
          this.sessionAffectedEntities.push(...chatMsg.data.affectedEntities);
        }
        return of(chatMsg);
      case 'chatTitleGenerated':
        this.handleChatTitleGenerated(chatMsg);
        return of(chatMsg);
      default:
        if (chatMsg.event === 'error') {
          this.showErrorToast(this.sanitizer.sanitize(SecurityContext.HTML, chatMsg.data?.message) ?? chatMsg.data?.message);
        }
        return of(chatMsg);
    }
  }

  private handleChatTitleGenerated(chatMsg: ChatTitleGeneratedEvent): void {
    const chatId = this.chatId();
    const title = chatMsg.data.title;
    if (!chatId || !title) return;
    this.panelService.updateChatTitle(chatId, title);
  }

  private handleToolApproval(chatMsg: ToolExecutionRequestedEvent): Observable<ChatEvent> {
    if (!chatMsg.data.needsApproval) {
      return of(chatMsg);
    }
    this.messages.update(list => [...list, {from: 'APPROVAL', data: chatMsg.data}]);
    return of(chatMsg);
  }

  private initLoadingState(): void {
    this.loading.set(true);
  }

  private stopLoadingState(): void {
    this.loading.set(false);
  }

  private showErrorMessage(error: any): void {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    const message = parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message;
    this.showErrorToast(message);
  }

  private showErrorToast(message: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message,
      target: 'aiAssistant',
      modern: true,
      type: 'error',
      verticalPosition: 'bottom',
      horizontalPosition: 'center'
    }));
  }
}
