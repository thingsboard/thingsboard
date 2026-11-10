// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable, signal, untracked, WritableSignal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { AiChatService } from '@core/http/ai-chat.service';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { DialogService } from '@core/services/dialog.service';
import { AnswerFeedback, ChatConfiguration, ChatInfo, ChatLevel } from '@shared/models/ai-chat.models';

@Injectable({ providedIn: 'root' })
export class AiChatsService {

  private aiChat = inject(AiChatService);
  private panelService = inject(AiAssistantPanelService);
  private dialog = inject(DialogService);
  private translate = inject(TranslateService);

  private _chats = signal<ChatInfo[]>([]);
  readonly chats = this._chats.asReadonly();

  private _loaded = signal(false);
  readonly loaded = this._loaded.asReadonly();

  private _replying = signal<ReadonlySet<string>>(new Set());
  readonly replying = this._replying.asReadonly();

  private _waiting = signal<ReadonlySet<string>>(new Set());

  private _open = signal<ReadonlySet<string>>(new Set());

  private _failed = signal<ReadonlyMap<string, string>>(new Map());

  levelOf(chat: ChatInfo): ChatLevel | null {
    if (this._waiting().has(chat.id)) {
      return 'needs';
    }
    if (this._replying().has(chat.id)) {
      return 'running';
    }
    if (this._failed().has(chat.id)) {
      return 'error';
    }
    if (this._open().has(chat.id)) {
      return chat.status === 'FAILED' ? 'error' : null;
    }
    return chat.status === 'NEEDS_YOU' ? 'needs' : chat.status === 'RUNNING' ? 'running' : chat.status === 'FAILED' ? 'error' : null;
  }

  needsAttention(chat: ChatInfo): boolean {
    const level = this.levelOf(chat);
    return level === 'needs' || level === 'error';
  }

  tipOf(chat: ChatInfo): string {
    const level = this.levelOf(chat);
    if (!level) {
      return '';
    }
    const reason = level === 'error' ? this._failed().get(chat.id) ?? chat.statusReason : undefined;
    return reason || this.translate.instant('ai-assistant.status-tip-' + level);
  }

  setFailed(chatId: string | undefined, reason: string): void {
    if (chatId) {
      this._failed.update(map => new Map(map).set(chatId, reason));
    }
  }

  clearFailure(chatId: string | undefined): void {
    if (!chatId) {
      return;
    }
    if (untracked(this._failed).has(chatId)) {
      this._failed.update(map => {
        const next = new Map(map);
        next.delete(chatId);
        return next;
      });
    }
    if (untracked(this._chats).some(chat => chat.id === chatId && chat.status === 'FAILED')) {
      this._chats.update(list => list.map(chat => chat.id === chatId && chat.status === 'FAILED'
        ? {...chat, status: undefined, statusReason: undefined} : chat));
    }
  }

  setWaiting(chatId: string | undefined, waiting: boolean): void {
    this.toggle(this._waiting, chatId, waiting);
  }

  setOpen(chatId: string | undefined, open: boolean): void {
    this.toggle(this._open, chatId, open);
  }

  refresh(): Observable<ChatInfo[]> {
    return this.aiChat.listChats({ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(chats => this._chats.set(chats))
    );
  }

  load(): Observable<ChatInfo[]> {
    return this.aiChat.listChats({ignoreLoading: true, ignoreErrors: true}).pipe(
      tap({
        next: chats => {
          this._chats.set(chats);
          this._loaded.set(true);
          this.panelService.setChats(chats);
          this.moveStoredAutoApprove(chats);
        },
        error: () => this._loaded.set(true)
      })
    );
  }

  find(chatId: string | undefined): ChatInfo | undefined {
    return chatId ? this._chats().find(chat => chat.id === chatId) : undefined;
  }

  create(title: string, autoApprove = false): Observable<ChatInfo> {
    const request: ChatConfiguration = autoApprove ? {title, autoApprove} : {title};
    return this.aiChat.createChat(request, {ignoreLoading: true, ignoreErrors: true}).pipe(
      map(id => ({id, title, createdTime: Date.now(), autoApprove} as ChatInfo)),
      tap(chat => {
        this._chats.update(list => [chat, ...list]);
        if (this.panelService.chats() !== null) {
          this.panelService.addChat(chat);
        }
      })
    );
  }

  rename(chatId: string, title: string): Observable<void> {
    return this.aiChat.updatedChat(chatId, {title}, {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(() => this.setTitle(chatId, title))
    );
  }

  sendFeedback(chatId: string, feedback: AnswerFeedback): Observable<void> {
    return this.aiChat.updatedChat(chatId, {feedback}, {ignoreLoading: true});
  }

  isAutoApprove(chatId: string | undefined): boolean {
    return !!this.find(chatId)?.autoApprove;
  }

  setAutoApprove(chatId: string, autoApprove: boolean): Observable<void> {
    const before = this.isAutoApprove(chatId);
    this.applyAutoApprove(chatId, autoApprove);
    return this.aiChat.updatedChat(chatId, {autoApprove}, {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap({error: () => this.applyAutoApprove(chatId, before)})
    );
  }

  private applyAutoApprove(chatId: string, autoApprove: boolean): void {
    this._chats.update(list => list.map(chat => chat.id === chatId ? {...chat, autoApprove} : chat));
    this.panelService.updateChatAutoApprove(chatId, autoApprove);
  }

  private moveStoredAutoApprove(chats: ChatInfo[]): void {
    const stored = new Set(this.panelService.takeStoredAutoApproveChats());
    chats.filter(chat => stored.has(chat.id) && !chat.autoApprove)
      .forEach(chat => this.setAutoApprove(chat.id, true).subscribe({error: () => {}}));
  }

  setPinned(chatId: string, pinned: boolean): Observable<void> {
    return this.aiChat.updatedChat(chatId, {pinned}, {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(() => this._chats.update(list => list.map(chat => chat.id === chatId ? {...chat, pinned} : chat)))
    );
  }

  setTitle(chatId: string, title: string): void {
    this._chats.update(list => list.map(chat => chat.id === chatId ? {...chat, title} : chat));
    this.panelService.updateChatTitle(chatId, title);
  }

  confirmAndDelete(chat: ChatInfo): Observable<boolean> {
    const title = chat.title || this.translate.instant('ai-assistant.new-chat');
    return this.dialog.confirm(
      this.translate.instant('ai-assistant.delete-chat-title', {name: title}),
      this.translate.instant('ai-assistant.delete-chat-text'),
      this.translate.instant('action.no'),
      this.translate.instant('ai-assistant.delete-chat-confirm')
    ).pipe(
      switchMap(confirmed => confirmed
        ? this.aiChat.deleteChat(chat.id, {ignoreLoading: true, ignoreErrors: true}).pipe(
          tap(() => {
            this._chats.update(list => list.filter(c => c.id !== chat.id));
            this.panelService.removeChat(chat.id);
          }),
          map(() => true)
        )
        : of(false))
    );
  }

  setReplying(chatId: string | undefined, replying: boolean): void {
    this.toggle(this._replying, chatId, replying);
  }

  private toggle(target: WritableSignal<ReadonlySet<string>>, chatId: string | undefined, on: boolean): void {
    if (!chatId || untracked(target).has(chatId) === on) {
      return;
    }
    target.update(set => {
      const next = new Set(set);
      if (on) {
        next.add(chatId);
      } else {
        next.delete(chatId);
      }
      return next;
    });
  }
}
