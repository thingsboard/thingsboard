// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  model,
  OnDestroy,
  OnInit,
  output,
  signal,
  untracked,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, of, Subject, throwError, timer } from 'rxjs';
import { catchError, concatMap, finalize, retry, switchMap, takeUntil, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { AiChatService } from '@core/http/ai-chat.service';
import { AiChatsService } from '@core/services/ai-chats.service';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { ActionChatMessage, AiStartedFrom, ApprovalChatMessage, ChatMessage, GeneralChatMessage } from '@shared/models/solution-creator.models';
import {
  AiAssistantPromptExample,
  AiChatClientContext,
  AiNotice,
  AiPromptMessage,
  ChatInfo,
  ChatLevel,
  ApprovalResult,
  ChatEvent,
  SendChatMessageRequest,
  AiAction,
  ToolExecutionRequestedEvent,
  ToolExecutionResult,
  ToolFinished,
  ToolStarted
} from '@shared/models/ai-chat.models';
import { aiActionWords, AiSpinKind, AiWorkMoment, AiWorkState, isHiddenAiAction, SUGGEST_PROMPTS_TOOL, ASK_USER_TOOL } from '@home/components/ai/ai-action.models';
import { AnswerRatingEvent, AnswerReportEvent } from '@home/components/ai/ai-chat-messages.component';
import { EntityId } from '@shared/models/id/entity-id';
import { AiChatTitleComponent } from '@home/components/ai/ai-chat-title.component';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';

export type AiChatVariant = 'page' | 'panel';

@Component({
  selector: 'tb-ai-chat',
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss'],
  standalone: false,
  host: {
    'data-ai': '',
    '[attr.data-type]': 'docked() ? "compact" : null',
    '[attr.data-space]': 'docked() ? "compact" : null',
    'class': 'tb-ai-chat',
    '[class.v-page]': '!docked()',
    '[class.v-panel]': 'docked()'
  }
})
export class AiChatComponent implements OnInit, OnDestroy {

  private static readonly RESUME_ATTEMPTS = 4;
  private static readonly RESUME_DELAY_MS = 2000;
  private static readonly END_IN_VIEW = 160;
  private static readonly NOTICE_MS = 6000;
  private static readonly RECENT_CHATS = 5;

  @ViewChild(AiPromptInputComponent) promptInput: AiPromptInputComponent;
  @ViewChild(AiChatTitleComponent) titleRef: AiChatTitleComponent;

  chatId = model<string | undefined>(undefined);

  variant = input<AiChatVariant>('page');

  clientContext = input<AiChatClientContext | undefined>(undefined);

  toastTarget = input<string>('aiChat');

  fullscreenEnabled = input<boolean>(true);

  prompts = input<AiAssistantPromptExample[]>([]);

  startTitle = input<string>();

  startLine = input<string>();

  startPlaceholder = input<string>();

  closed = output<void>();

  recentChats = computed(() => this.chats.chats()
    .filter(chat => chat.id !== this.chatId())
    .sort((a, b) => b.createdTime - a.createdTime)
    .slice(0, AiChatComponent.RECENT_CHATS));

  fullscreen = signal<boolean>(false);

  docked = computed(() => this.variant() === 'panel' && !this.fullscreen());

  readonly markdownStyles = [
    '.tb-markdown-view p, .tb-markdown-view h2, .tb-markdown-view h3, .tb-markdown-view h4, .tb-markdown-view h5, ' +
    '.tb-markdown-view h6, .tb-markdown-view div.code-wrapper {padding-left: 0 !important;} ' +
    '.tb-markdown-view ol, .tb-markdown-view ul {padding-left: 24px; padding-inline-start: 24px; padding-right: 0;}'
  ];

  messages = signal<ChatMessage[]>([]);

  loading = signal<boolean>(false);

  completedExecutions = signal<Record<string, ToolExecutionResult>>({});



  chat = computed(() => {
    const id = this.chatId();
    return id ? this.chats.chats().find(chat => chat.id === id) : undefined;
  });

  newChatAutoApprove = signal<boolean>(false);

  autoApprove = computed(() => this.chatId() ? this.panelService.isAutoApprove(this.chatId()) : this.newChatAutoApprove());

  denyReasons = signal<Record<string, string>>({});

  stopping = signal<boolean>(false);

  work = signal<AiWorkState | undefined>(undefined);

  readonly tasks = ['connect-device', 'build-dashboard', 'set-up-alarm'];

  scrolled = signal(false);

  private atEnd = signal(true);

  private newTurns = signal(0);

  notice = signal<AiNotice | null>(null);

  private noticeTimer: ReturnType<typeof setTimeout>;

  private seenMessages = 0;

  pill = computed<{mode: 'work' | 'new'; kind: AiSpinKind; label: string; count: number} | null>(() => {
    if (this.atEnd()) {
      return null;
    }
    if (this.loading() && !this.waitingApproval()) {
      const running = this.messages().filter((message): message is ActionChatMessage =>
        message.from === 'ACTION' && message.data.status === 'RUNNING');
      if (running.length) {
        const words = aiActionWords(running[0].data.tool, running[0].data.arguments);
        return {mode: 'work', kind: words.kind, label: this.translate.instant(words.key + '.run', words.params), count: 0};
      }
      const work = this.work();
      return {mode: 'work', kind: 'think', label: work ? this.translate.instant('ai-assistant.work.' + work.moment) : '', count: 0};
    }
    const count = this.newTurns();
    return count ? {mode: 'new', kind: 'think', label: this.translate.instant('ai-assistant.new-count', {count}), count} : null;
  });

  private answeredApprovals = signal<ReadonlySet<string>>(new Set());

  waitingApproval = computed<ApprovalChatMessage | undefined>(() => {
    if (this.autoApprove()) {
      return undefined;
    }
    const completed = this.completedExecutions();
    const answered = this.answeredApprovals();
    return this.messages()
      .filter((message): message is ApprovalChatMessage => message.from === 'APPROVAL')
      .find(message => !completed[message.data.executionId] && !answered.has(message.data.executionId) && message.data.needsApproval);
  });

  placeholder = computed(() => !this.waitingApproval() && !this.messages().length && this.startPlaceholder()
    ? this.startPlaceholder()
    : this.translate.instant(this.waitingApproval()
      ? 'ai-assistant.deny-placeholder'
      : this.messages().length
        ? 'ai-assistant.follow-up-prompt-placeholder'
        : 'ai-assistant.prompt-placeholder'));

  private aiChat = inject(AiChatService);
  private chats = inject(AiChatsService);
  private panelService = inject(AiAssistantPanelService);
  private translate = inject(TranslateService);
  private store = inject(Store<AppState>);
  private sanitizer = inject(DomSanitizer);
  private destroyRef = inject(DestroyRef);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private router = inject(Router);

  readonly hostElement = this.host.nativeElement;

  private loadChat$ = new Subject<string>();
  private cancelMessage$ = new Subject<void>();
  private messageCancelled = false;
  private shownChatId: string | undefined;
  private replyingChatId: string | undefined;
  private affectedEntities: EntityId[] = [];
  private logScrollBottom: number | undefined;

  private newTurnsEffect = effect(() => {
    this.messages();
    untracked(() => this.countNewTurns());
  });

  private chatIdEffect = effect(() => {
    const chatId = this.chatId();
    untracked(() => this.show(chatId));
  });

  level = computed(() => {
    const chat = this.chat();
    return chat ? this.chats.levelOf(chat) : null;
  });

  statusTip = computed(() => {
    const chat = this.chat();
    return chat ? this.chats.tipOf(chat) : '';
  });

  private openChatEffect = effect(onCleanup => {
    const chatId = this.chatId();
    untracked(() => this.chats.setOpen(chatId, true));
    onCleanup(() => untracked(() => this.chats.setOpen(chatId, false)));
  });

  private waitingEffect = effect(onCleanup => {
    const chatId = this.chatId();
    const waiting = !!this.waitingApproval();
    untracked(() => this.chats.setWaiting(chatId, waiting));
    onCleanup(() => untracked(() => this.chats.setWaiting(chatId, false)));
  });

  private deletedChatEffect = effect(() => {
    const chatId = this.chatId();
    if (chatId && this.chats.loaded() && !this.chats.find(chatId)) {
      untracked(() => this.chatId.set(undefined));
    }
  });

  ngOnInit(): void {
    this.loadChat$.pipe(
      switchMap(id => this.aiChat.getChatMessages(id, {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(err => {
          this.showErrorMessage(err);
          return EMPTY;
        })
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(messages => {
      this.showHistory(messages);
      this.resumeTurn(this.chatId());
    });
  }

  ngOnDestroy(): void {
    this.cancelActiveMessage();
  }

  toggleFullscreen(): void {
    const log = this.host.nativeElement.querySelector('.ai-chat-body');
    this.logScrollBottom = log ? log.scrollHeight - log.clientHeight - log.scrollTop : undefined;
    this.fullscreen.set(!this.fullscreen());
  }

  onFullscreenChanged(fullscreen: boolean): void {
    this.fullscreen.set(fullscreen);
    const scrollBottom = this.logScrollBottom;
    if (scrollBottom !== undefined) {
      setTimeout(() => {
        const log = this.host.nativeElement.querySelector('.ai-chat-body');
        if (log) {
          log.scrollTop = log.scrollHeight - log.clientHeight - scrollBottom;
        }
      });
    }
  }

  focus(): void {
    this.promptInput?.focus();
  }

  levelOf(chat: ChatInfo): ChatLevel | null {
    return this.chats.levelOf(chat);
  }

  newChat(): void {
    this.chatId.set(undefined);
    setTimeout(() => this.focus());
  }

  ask(message: string): void {
    this.sendMsgFromUser({message, resources: []});
  }

  openAllChats(): void {
    this.router.navigate(['/ai-assistant']);
    this.closed.emit();
  }

  startRename(): void {
    this.titleRef?.startRename();
  }

  rename(title: string): void {
    const chat = this.chat();
    if (chat) {
      this.chats.rename(chat.id, title).subscribe({error: err => this.showErrorMessage(err)});
    }
  }

  togglePinned(): void {
    const chat = this.chat();
    if (chat) {
      this.chats.setPinned(chat.id, !chat.pinned).subscribe({error: err => this.showErrorMessage(err)});
    }
  }

  deleteChat(): void {
    const chat = this.chat();
    if (chat) {
      this.chats.confirmAndDelete(chat).subscribe({error: err => this.showErrorMessage(err)});
    }
  }

  setAutoApprove(enabled: boolean): void {
    const chatId = this.chatId();
    if (chatId) {
      this.panelService.setAutoApprove(chatId, enabled);
    } else {
      this.newChatAutoApprove.set(enabled);
    }
  }

  stop(): void {
    const chatId = this.chatId();
    if (!chatId || !this.loading() || this.stopping()) {
      return;
    }
    this.stopping.set(true);
    this.aiChat.stopChat(chatId, {ignoreLoading: true, ignoreErrors: true}).subscribe({
      error: err => {
        this.stopping.set(false);
        this.showErrorMessage(err);
      }
    });
  }

  onApprovalAction(event: ApprovalResult): void {
    const chatId = this.chatId();
    if (event.autoApprove && chatId) {
      this.panelService.setAutoApprove(chatId, true);
    }
    this.answeredApprovals.update(set => new Set(set).add(event.executionId));
    this.setWork(event.approved ? 'applying-change' : event.reason ? 'reading-what-to-change' : 'going-through-results');
    if (event.reason) {
      this.denyReasons.update(reasons => ({...reasons, [event.executionId]: event.reason}));
    }
    const result: ApprovalResult = {executionId: event.executionId, approved: event.approved};
    if (event.reason) {
      result.reason = event.reason;
    }
    this.aiChat.resolveToolApproval(result, {ignoreLoading: true, ignoreErrors: true}).subscribe();
  }

  startTask(task: string): void {
    this.promptInput?.setValue(this.translate.instant('ai-assistant.task.' + task + '.prompt'));
  }

  onRate(event: AnswerRatingEvent): void {
    const answer = this.answerAt(event.index);
    if (!answer) {
      return;
    }
    const before = answer.rating;
    const rating = event.rating === 'NONE' ? undefined : event.rating;
    this.patchAnswer(answer, {rating});
    this.chats.sendFeedback(this.chatId(), {answer: answer.content, rating: event.rating}).subscribe({
      error: () => {
        this.patchAnswer(answer, {rating: before});
        this.showErrorToast(this.translate.instant('ai-assistant.feedback.failed'));
      }
    });
  }

  onReport(event: AnswerReportEvent): void {
    const answer = this.answerAt(event.index);
    if (!answer) {
      return;
    }
    this.patchAnswer(answer, {reported: true});
    this.chats.sendFeedback(this.chatId(), {answer: answer.content, problem: event.problem, comment: event.comment}).subscribe({
      next: () => this.showToast(this.translate.instant('ai-assistant.feedback.thanks'), 'success'),
      error: () => {
        this.patchAnswer(answer, {reported: false});
        this.showErrorToast(this.translate.instant('ai-assistant.feedback.failed'));
      }
    });
  }

  private answerAt(index: number): GeneralChatMessage | null {
    const message = this.messages()[index];
    return this.chatId() && message?.from === 'AI' && message.content ? message : null;
  }

  private patchAnswer(answer: GeneralChatMessage, patch: Partial<GeneralChatMessage>): void {
    this.messages.update(list => list.map(message => message === answer || (message.from === 'AI' && message.content === answer.content
      && message.time === answer.time) ? {...message, ...patch} as GeneralChatMessage : message));
  }

  onAskAgain(): void {
    if (!this.loading()) {
      this.sendMsgFromUser({message: this.translate.instant('ai-assistant.review.ask-again-message'), resources: []});
    }
  }

  sendMsgFromUser(prompt: AiPromptMessage): void {
    const waiting = this.waitingApproval();
    if (waiting) {
      if (prompt.message.trim()) {
        this.onApprovalAction({executionId: waiting.data.executionId, approved: false, reason: prompt.message.trim()});
      }
      return;
    }
    const userMessage: GeneralChatMessage = prompt.resources.length
      ? {from: 'USER', content: prompt.message, resources: prompt.resources, time: Date.now()}
      : {from: 'USER', content: prompt.message, time: Date.now()};
    const startedFrom = this.messages().length ? undefined : this.startingPage();
    if (startedFrom) {
      userMessage.startedFrom = startedFrom;
    }
    this.clearPassingNotice();
    this.messages.update(list => [...list, userMessage]);
    if (this.loading() && this.chatId()) {
      this.sendWhileWorking(prompt, userMessage);
      return;
    }
    this.sendMessage(prompt).subscribe({
      error: err => {
        if (this.isConnectionLost(err) && this.chatId()) {
          this.setNotice('warn', 'wifi_off', 'connection-lost');
          this.resumeTurn(this.chatId(), true);
          return;
        }
        this.messages.update(list => list.filter(message => message !== userMessage));
        setTimeout(() => this.showErrorMessage(err), 0);
      }
    });
  }

  private startingPage(): AiStartedFrom | undefined {
    const view = this.clientContext()?.view;
    if (!view?.type || this.variant() !== 'panel') {
      return undefined;
    }
    if (view.entityId) {
      return {type: view.type, entityId: view.entityId};
    }
    return view.type.endsWith('_LIST') || view.type === 'NOTIFICATION_INBOX' ? {type: view.type} : undefined;
  }

  private show(chatId: string | undefined): void {
    if (chatId === this.shownChatId) {
      return;
    }
    this.cancelActiveMessage();
    this.titleRef?.cancelRename();
    this.shownChatId = chatId;
    this.resetLogPosition(0);
    this.messages.set([]);
    this.completedExecutions.set({});
    this.denyReasons.set({});
    this.answeredApprovals.set(new Set());
    if (chatId) {
      this.loadChat$.next(chatId);
    } else {
      setTimeout(() => this.promptInput?.focus());
    }
  }

  resume(): void {
    this.sendMsgFromUser({message: this.translate.instant('ai-assistant.resume'), resources: []});
  }

  private sendWhileWorking(prompt: AiPromptMessage, userMessage: ChatMessage): void {
    const chatId = this.chatId();
    let ownTurn = false;
    this.aiChat.sendChatMessage(chatId, this.chatRequest(prompt), {ignoreLoading: true, ignoreErrors: true}).pipe(
      takeUntil(this.cancelMessage$),
      concatMap(event => {
        if (event.event === 'messageQueued') {
          return of(event);
        }
        if (!ownTurn && !this.loading()) {
          ownTurn = true;
          this.chats.clearFailure(chatId);
          this.startLoading();
        }
        return this.handleChatEvent(event);
      }),
      finalize(() => {
        if (ownTurn) {
          this.stopLoading();
        }
      })
    ).subscribe({
      error: err => {
        this.messages.update(list => list.filter(message => message !== userMessage));
        setTimeout(() => this.showErrorMessage(err), 0);
      }
    });
  }

  private chatRequest(prompt: AiPromptMessage): SendChatMessageRequest {
    const request: SendChatMessageRequest = {message: prompt.message};
    if (prompt.resources.length) {
      request.resources = prompt.resources.map(resource => resource.id);
    }
    if (this.clientContext()) {
      request.clientContext = this.clientContext();
    }
    return request;
  }

  private sendMessage(prompt: AiPromptMessage): Observable<ChatEvent> {
    this.messageCancelled = false;
    const startOrContinue$ = this.chatId()
      ? of(null)
      : this.chats.create('New chat').pipe(
        tap(chat => {
          if (this.newChatAutoApprove()) {
            this.panelService.setAutoApprove(chat.id, true);
          }
          this.shownChatId = chat.id;
          this.chatId.set(chat.id);
        })
      );
    const request = this.chatRequest(prompt);
    this.setWork('reading-request', true);
    this.loading.set(true);
    return startOrContinue$.pipe(
      tap(() => {
        this.chats.clearFailure(this.chatId());
        this.startLoading();
      }),
      switchMap(() => this.aiChat.sendChatMessage(this.chatId(), request, {ignoreLoading: true, ignoreErrors: true})),
      takeUntil(this.cancelMessage$),
      concatMap(event => this.handleChatEvent(event)),
      finalize(() => this.stopLoading())
    );
  }

  private resumeTurn(chatId: string, afterConnectionLoss = false): void {
    if (!chatId) {
      return;
    }
    let resumed = false;
    this.messageCancelled = false;
    const attempt$ = timer(afterConnectionLoss ? AiChatComponent.RESUME_DELAY_MS : 0).pipe(
      tap(() => {
        if (afterConnectionLoss) {
          this.startLoading();
        }
      }),
      switchMap(() => this.aiChat.resumeChat(chatId, {ignoreLoading: true, ignoreErrors: true}))
    );
    attempt$.pipe(
      retry({
        count: afterConnectionLoss ? AiChatComponent.RESUME_ATTEMPTS : 0,
        delay: (error, attempt) => this.isConnectionLost(error)
          ? timer(attempt * AiChatComponent.RESUME_DELAY_MS)
          : throwError(() => error)
      }),
      takeUntil(this.cancelMessage$),
      concatMap(event => {
        if (event.event === 'turnResumed') {
          resumed = true;
          if (afterConnectionLoss) {
            this.setNotice('info', 'wifi', 'connected-again', AiChatComponent.NOTICE_MS);
          }
          this.startLoading();
          this.dropRunningReply();
          return of(event);
        }
        return this.handleChatEvent(event);
      }),
      finalize(() => {
        if (resumed || afterConnectionLoss) {
          this.stopLoading();
        }
      })
    ).subscribe({
      error: () => {
        if (afterConnectionLoss) {
          this.setNotice('warn', 'wifi_off', 'reconnect-failed');
        }
      },
      complete: () => {
        if (this.messageCancelled || this.chatId() !== chatId) {
          return;
        }
        if (afterConnectionLoss && !resumed) {
          this.setNotice('info', 'wifi', 'connected-again', AiChatComponent.NOTICE_MS);
        }
        if (resumed || afterConnectionLoss) {
          this.aiChat.getChatMessages(chatId, {ignoreLoading: true, ignoreErrors: true})
            .pipe(takeUntil(this.cancelMessage$))
            .subscribe(messages => this.showHistory(messages));
        }
      }
    });
  }

  private handleChatEvent(event: ChatEvent): Observable<ChatEvent> {
    switch (event.event) {
      case 'assistantMessage':
        this.messages.update(list => [...list, {
          from: 'AI', content: event.data.message, time: Date.now(),
          ...(event.data.references?.length ? {references: event.data.references} : {}),
          ...(event.data.severities?.length ? {severities: event.data.severities} : {})
        }]);
        break;
      case 'toolExecutionRequested':
        this.handleToolApproval(event);
        break;
      case 'toolExecutionResult':
        this.completedExecutions.update(map => ({...map, [event.data.executionId]: event.data}));
        if (event.data.affectedEntities?.length) {
          this.affectedEntities.push(...event.data.affectedEntities);
        }
        break;
      case 'toolStarted':
        this.startAction(event.data);
        break;
      case 'toolFinished':
        this.finishAction(event.data);
        break;
      case 'turnStopped':
        this.messages.update(list => [...list, {from: 'AI', content: this.translate.instant('ai-assistant.stopped'), status: 'STOPPED', time: Date.now()}]);
        break;
      case 'chatTitleGenerated':
        if (this.chatId() && event.data.title) {
          this.chats.setTitle(this.chatId(), event.data.title);
        }
        break;
      default:
        if (event.event === 'error' && event.data?.code) {
          this.restoreLastPrompt();
          this.setNotice(event.data.code === 'CREDITS_USED_UP' ? 'block' : 'warn',
            event.data.code === 'CREDITS_USED_UP' ? 'block' : 'hourglass_top',
            event.data.code === 'CREDITS_USED_UP' ? 'credits-used-up' : 'rate-limited');
        } else if (event.event === 'error') {
          const message = event.data?.message || this.translate.instant('ai-assistant.run-failed');
          this.chats.setFailed(this.chatId(), message);
          this.messages.update(list => [...list, {from: 'AI', content: message, status: 'INCOMPLETE', time: Date.now()}]);
        }
    }
    return of(event);
  }

  onLogScroll(area: HTMLElement): void {
    const scrolled = area.scrollTop > 0;
    if (scrolled !== this.scrolled()) {
      this.scrolled.set(scrolled);
    }
    const atEnd = area.scrollHeight - area.scrollTop - area.clientHeight < AiChatComponent.END_IN_VIEW;
    if (atEnd !== this.atEnd()) {
      this.atEnd.set(atEnd);
    }
    if (atEnd) {
      this.newTurns.set(0);
      this.seenMessages = this.messages().length;
    }
  }

  goToEnd(area: HTMLElement): void {
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    area.scrollTo({top: area.scrollHeight, behavior: reduce ? 'auto' : 'smooth'});
  }

  private countNewTurns(): void {
    const total = this.messages().length;
    if (this.atEnd() || total <= this.seenMessages) {
      this.seenMessages = total;
      return;
    }
    const added = total - this.seenMessages;
    this.seenMessages = total;
    if (added) {
      this.newTurns.update(count => count + added);
    }
  }

  private startAction(started: ToolStarted): void {
    if (isHiddenAiAction(started.tool) || this.isApprovalExecution(started.executionId)) {
      return;
    }
    const action: AiAction = {
      executionId: started.executionId,
      tool: started.tool,
      arguments: started.arguments,
      status: started.tool === SUGGEST_PROMPTS_TOOL || started.tool === ASK_USER_TOOL ? 'SUCCESS' : 'RUNNING',
      startedAt: Date.now()
    };
    this.messages.update(list => [...list, {from: 'ACTION', data: action}]);
  }

  private finishAction(finished: ToolFinished): void {
    if (finished.affectedEntities?.length) {
      this.affectedEntities.push(...finished.affectedEntities);
    }
    this.messages.update(list => list.map(message => message.from === 'ACTION' && message.data.executionId === finished.executionId
      ? {from: 'ACTION', data: {...message.data, status: finished.status, durationMs: finished.durationMs, result: finished.result,
          affectedEntities: finished.affectedEntities}}
      : message));
    const prompts = this.messages().some(message => message.from === 'ACTION' && message.data.executionId === finished.executionId
      && (message.data.tool === SUGGEST_PROMPTS_TOOL || message.data.tool === ASK_USER_TOOL));
    if (!prompts && !this.messages().some(message => message.from === 'ACTION' && message.data.status === 'RUNNING')) {
      this.setWork('going-through-results', true);
    }
  }

  private isApprovalExecution(executionId: string): boolean {
    return this.messages().some(message => message.from === 'APPROVAL' && message.data.executionId === executionId);
  }

  private stopRunningActions(): void {
    if (this.messages().some(message => message.from === 'ACTION' && message.data.status === 'RUNNING')) {
      this.messages.update(list => list.map(message => message.from === 'ACTION' && message.data.status === 'RUNNING'
        ? {from: 'ACTION', data: {...message.data, status: 'STOPPED', durationMs: Date.now() - message.data.startedAt}}
        : message));
    }
  }

  private setWork(moment: AiWorkMoment, restart = false): void {
    if (restart || this.work()?.moment !== moment) {
      this.work.set({moment, since: Date.now()});
    }
  }

  private handleToolApproval(event: ToolExecutionRequestedEvent): void {
    if (event.data.needsApproval) {
      this.messages.update(list => [...list, {from: 'APPROVAL', data: event.data}]);
    }
  }

  private showHistory(messages: ChatMessage[]): void {
    const completed: Record<string, ToolExecutionResult> = {};
    for (const message of messages) {
      const result = message.from === 'APPROVAL' ? (message as ApprovalChatMessage).result : undefined;
      if (result) {
        completed[result.executionId] = result;
      }
    }
    this.completedExecutions.set(completed);
    this.resetLogPosition(messages.length);
    this.messages.set(messages);
  }

  private resetLogPosition(seen: number): void {
    this.atEnd.set(true);
    this.scrolled.set(false);
    this.newTurns.set(0);
    this.seenMessages = seen;
  }

  private dropRunningReply(): void {
    this.messages.update(list => {
      const lastUser = list.map(message => message.from).lastIndexOf('USER');
      return lastUser >= 0 ? list.slice(0, lastUser + 1) : list;
    });
  }

  private cancelActiveMessage(): void {
    this.messageCancelled = true;
    this.cancelMessage$.next();
    this.stopLoading();
    this.promptInput?.reset();
  }

  private startLoading(): void {
    this.loading.set(true);
    this.setWork('reading-request');
    this.chats.setReplying(this.replyingChatId, false);
    this.replyingChatId = this.chatId();
    this.chats.setReplying(this.replyingChatId, true);
  }

  private stopLoading(): void {
    if (this.loading()) {
      this.chats.refresh().subscribe({error: () => {}});
    }
    this.loading.set(false);
    this.stopping.set(false);
    this.work.set(undefined);
    this.stopRunningActions();
    if (this.affectedEntities.length) {
      this.panelService.emitUpdatedData(this.affectedEntities);
      this.affectedEntities = [];
    }
    this.chats.setReplying(this.replyingChatId, false);
    this.replyingChatId = undefined;
  }

  private isConnectionLost(error: any): boolean {
    return error instanceof HttpErrorResponse && error.status === 0;
  }

  private showErrorMessage(error: any): void {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    this.showErrorToast(parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message);
  }

  private setNotice(kind: AiNotice['kind'], icon: string, key: string, hideAfter?: number): void {
    clearTimeout(this.noticeTimer);
    const notice: AiNotice = {kind, icon, text: this.translate.instant('ai-assistant.notice.' + key)};
    this.notice.set(notice);
    if (hideAfter) {
      this.noticeTimer = setTimeout(() => {
        if (this.notice() === notice) {
          this.notice.set(null);
        }
      }, hideAfter);
    }
  }

  private clearPassingNotice(): void {
    if (this.notice()?.kind !== 'block') {
      clearTimeout(this.noticeTimer);
      this.notice.set(null);
    }
  }

  private restoreLastPrompt(): void {
    const messages = this.messages();
    const last = messages.map(message => message.from).lastIndexOf('USER');
    if (last < 0) {
      return;
    }
    const content = (messages[last] as GeneralChatMessage).content;
    this.messages.set(messages.slice(0, last));
    if (content) {
      setTimeout(() => this.promptInput?.setValue(content));
    }
  }

  private showErrorToast(message: string): void {
    this.showToast(message, 'error');
  }

  private showToast(message: string, type: 'error' | 'success'): void {
    this.store.dispatch(new ActionNotificationShow({
      message,
      target: this.toastTarget(),
      modern: true,
      type,
      verticalPosition: 'bottom',
      horizontalPosition: 'center'
    }));
  }
}
