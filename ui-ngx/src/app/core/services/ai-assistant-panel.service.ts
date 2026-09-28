// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { computed, DestroyRef, inject, Injectable, OnDestroy, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AiAssistantPanelConfig,
  AiAssistantViewType,
  AiChatClientContext,
  AiChatView,
  ChatInfo
} from '@shared/models/ai-chat.models';
import { EntityId } from '@shared/models/id/entity-id';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuthUser } from '@core/auth/auth.selectors';

@Injectable({ providedIn: 'root' })
export class AiAssistantPanelService implements OnDestroy {

  private _open = signal(false);
  readonly open = this._open.asReadonly();

  private _enabled = signal(false);
  readonly enabled = this._enabled.asReadonly();

  private _config = signal<AiAssistantPanelConfig | null>(null);
  readonly config = this._config.asReadonly();

  private _detailsOpen = signal(false);
  readonly detailsOpen = this._detailsOpen.asReadonly();

  private _clientContext = signal<AiChatClientContext | null>(null);
  readonly clientContext = this._clientContext.asReadonly();

  private _sentClientContextByChat = new Map<string, AiChatClientContext>();

  private _chats = signal<ChatInfo[] | null>(null);
  readonly chats = this._chats.asReadonly();

  private _lastChatIdByContext = new Map<string, string>();

  private static readonly AUTO_APPROVE_STORAGE_KEY = 'aiAssistantAutoApproveChats';

  private localStorage = inject(LocalStorageService);
  private store = inject(Store<AppState>);
  private destroyRef = inject(DestroyRef);

  private _autoApproveUserId: string | null = null;
  private _autoApproveChats = signal<Set<string>>(new Set());

  private _autoApproveUserSync = this.store.select(selectAuthUser).pipe(
    map(user => user?.userId ?? null),
    distinctUntilChanged(),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(userId => {
    this._autoApproveUserId = userId;
    this._autoApproveChats.set(this.loadAutoApproveChats(userId));
  });

  private _updatedData$ = new Subject<Array<EntityId>>();
  readonly updatedData$ = this._updatedData$.asObservable();

  private _teardownTimer: NodeJS.Timeout;

  toggle(): void {
    if (!this._enabled()) {
      return;
    }
    this._open.update(open => !open);
  }

  setChats(chats: ChatInfo[]): void {
    this._chats.set(chats);
    this.pruneAutoApproveChats(chats);
  }

  addChat(chat: ChatInfo): void {
    this._chats.update(list => [chat, ...(list ?? [])]);
  }

  updateChatTitle(chatId: string, title: string): void {
    this._chats.update(list => list?.map(c => c.id === chatId ? {...c, title} : c) ?? null);
  }

  removeChat(chatId: string): void {
    this._chats.update(list => list?.filter(c => c.id !== chatId) ?? null);
    this._sentClientContextByChat.delete(chatId);
    for (const [key, id] of this._lastChatIdByContext) {
      if (id === chatId) {
        this._lastChatIdByContext.delete(key);
      }
    }
    this.setAutoApprove(chatId, false);
  }

  isAutoApprove(chatId: string | undefined): boolean {
    return !!chatId && this._autoApproveChats().has(chatId);
  }

  setAutoApprove(chatId: string, enabled: boolean): void {
    this._autoApproveChats.update(chats => {
      const next = new Set(chats);
      if (enabled) {
        next.add(chatId);
      } else {
        next.delete(chatId);
      }
      return next;
    });
    this.persistAutoApproveChats();
  }

  private pruneAutoApproveChats(chats: ChatInfo[]): void {
    const valid = new Set(chats.map(c => c.id));
    const current = this._autoApproveChats();
    const pruned = new Set([...current].filter(id => valid.has(id)));
    if (pruned.size !== current.size) {
      this._autoApproveChats.set(pruned);
      this.persistAutoApproveChats();
    }
  }

  private autoApproveStorageKey(userId: string | null): string {
    return `${AiAssistantPanelService.AUTO_APPROVE_STORAGE_KEY}_${userId ?? 'anonymous'}`;
  }

  private loadAutoApproveChats(userId: string | null): Set<string> {
    try {
      const ids = this.localStorage.getItem(this.autoApproveStorageKey(userId));
      return new Set(Array.isArray(ids) ? ids as string[] : []);
    } catch {
      return new Set();
    }
  }

  private persistAutoApproveChats(): void {
    try {
      this.localStorage.setItem(this.autoApproveStorageKey(this._autoApproveUserId), [...this._autoApproveChats()]);
    } catch {
      // ignore storage write errors (e.g. private mode quota)
    }
  }

  // Identifies the assistant "session" a chat belongs to. List and entity-detail views of the
  // same domain collapse to a single key (they are one master-detail page), so focusing an entity
  // does not switch the chat. Dashboards are keyed per id, since each is a separate full-page context.
  readonly contextKey = computed<string | null>(() => {
    const view = this._clientContext()?.view;
    if (!view) {
      return null;
    }
    switch (view.type) {
      case AiAssistantViewType.DASHBOARD:
        return view.entityId ? `DASHBOARD:${view.entityId.id}` : 'DASHBOARD';
      case AiAssistantViewType.ALARM_RULE:
      case AiAssistantViewType.ALARM_RULE_LIST:
      case AiAssistantViewType.ALARM_LIST:
        return 'ALARM_RULE';
      case AiAssistantViewType.CALCULATED_FIELD:
      case AiAssistantViewType.CALCULATED_FIELD_LIST:
        return 'CALCULATED_FIELD';
      case AiAssistantViewType.DEVICE:
      case AiAssistantViewType.DEVICE_LIST:
        return 'DEVICE';
      case AiAssistantViewType.DEVICE_GROUP:
      case AiAssistantViewType.DEVICE_GROUP_LIST:
        return 'DEVICE_GROUP';
      case AiAssistantViewType.NOTIFICATION_INBOX:
      case AiAssistantViewType.SENT_NOTIFICATION_LIST:
      case AiAssistantViewType.NOTIFICATION_TEMPLATE_LIST:
      case AiAssistantViewType.NOTIFICATION_RECIPIENT_LIST:
      case AiAssistantViewType.NOTIFICATION_RULE_LIST:
        return 'NOTIFICATION';
      default:
        return view.type;
    }
  });

  setLastChatId(id: string | undefined): void {
    const key = this.contextKey();
    if (!key) {
      return;
    }
    if (id) {
      this._lastChatIdByContext.set(key, id);
    } else {
      this._lastChatIdByContext.delete(key);
    }
  }

  lastChatIdForContext(): string | undefined {
    const key = this.contextKey();
    return key ? this._lastChatIdByContext.get(key) : undefined;
  }

  close(): void {
    this._open.set(false);
  }

  openPanel(): void {
    this._open.set(true);
  }

  setEnabled(enabled: boolean): void {
    this._enabled.set(enabled);
    if (enabled) {
      clearTimeout(this._teardownTimer);
    }
  }

  setConfig(config: AiAssistantPanelConfig | null): void {
    this._config.set(config);
  }

  setDetailsOpen(open: boolean): void {
    this._detailsOpen.set(open);
  }

  setClientContext(ctx: AiChatClientContext | null): void {
    this._clientContext.set(ctx);
  }

  setClientContextForView(view: AiChatView): void {
    this.setClientContext({view, timeZone: getDefaultTimezone()});
  }

  getSentClientContext(chatId: string): AiChatClientContext | undefined {
    return this._sentClientContextByChat.get(chatId);
  }

  markClientContextSent(chatId: string, ctx: AiChatClientContext): void {
    this._sentClientContextByChat.set(chatId, ctx);
  }

  ngOnDestroy(): void {
    this._updatedData$.complete();
  }

  emitUpdatedData(affectedEntities: Array<EntityId>): void {
    this._updatedData$.next(affectedEntities);
  }

  teardown(): void {
    this._enabled.set(false);
    this._detailsOpen.set(false);
    this._clientContext.set(null);
    this._sentClientContextByChat.clear();
    this._chats.set(null);
    this._teardownTimer = setTimeout(() => {
      this._config.set(null);
      this._open.set(false);
    });
  }
}
