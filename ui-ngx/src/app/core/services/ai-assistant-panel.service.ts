// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { computed, DestroyRef, inject, Injectable, OnDestroy, signal } from '@angular/core';
import { defer, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, filter, finalize, map } from 'rxjs/operators';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
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

  private static readonly WITHOUT_PANEL = ['/home', '/ai-assistant'];

  private _open = signal(false);
  readonly open = this._open.asReadonly();

  private _suppressed = signal(false);
  readonly suppressed = this._suppressed.asReadonly();

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
  private router = inject(Router);

  private routeWatch = (() => {
    this._suppressed.set(AiAssistantPanelService.withoutPanel(this.router.url));
    this.router.events.pipe(
      filter((event): event is NavigationStart => event instanceof NavigationStart),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(event => {
      const suppressed = AiAssistantPanelService.withoutPanel(event.url);
      this._suppressed.set(suppressed);
      if (suppressed) {
        this._open.set(false);
      }
    });
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(event => {
      const suppressed = AiAssistantPanelService.withoutPanel(event.urlAfterRedirects);
      this._suppressed.set(suppressed);
      const openHere = this._openOnArrival && !suppressed;
      if (!suppressed) {
        this._openOnArrival = false;
      }
      if (openHere) {
        this._open.set(true);
      }
    });
  })();

  private static withoutPanel(url: string): boolean {
    const path = (url || '/').split(/[?#]/)[0];
    return path === '/' || AiAssistantPanelService.WITHOUT_PANEL.some(page => path === page || path.startsWith(page + '/'));
  }

  private _autoApproveUserId: string | null = null;

  private _autoApproveUserSync = this.store.select(selectAuthUser).pipe(
    map(user => user?.userId ?? null),
    distinctUntilChanged(),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(userId => this._autoApproveUserId = userId);

  private _updatedData$ = new Subject<Array<EntityId>>();
  readonly updatedData$ = this._updatedData$.asObservable();

  private _teardownTimer: NodeJS.Timeout;

  private static readonly CARRY_MS = 15000;
  private _carried: {chatId: string; from: string | null; at: number} | null = null;

  private static readonly FROM_AI_PAGE = 'ai-assistant-page';

  private _openOnArrival = false;

  carryChat(chatId: string): void {
    this._carried = {chatId, from: this.contextKey(), at: Date.now()};
  }

  carryChatToPage(chatId: string): void {
    this._carried = {chatId, from: AiAssistantPanelService.FROM_AI_PAGE, at: Date.now()};
    this._openOnArrival = true;
  }

  hasCarriedChat(): boolean {
    return !!this._carried;
  }

  takeCarriedChat(): string | undefined {
    const carried = this._carried;
    const key = this.contextKey();
    const fromPage = carried?.from === AiAssistantPanelService.FROM_AI_PAGE;
    if (!carried || (key === null && !fromPage) || key === carried.from) {
      return undefined;
    }
    this._carried = null;
    if (Date.now() - carried.at > AiAssistantPanelService.CARRY_MS) {
      return undefined;
    }
    if (key !== null) {
      this._lastChatIdByContext.set(key, carried.chatId);
    }
    return carried.chatId;
  }

  toggle(): void {
    this._open.update(open => !open);
  }

  setChats(chats: ChatInfo[]): void {
    this._chats.set(chats);
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
  }

  updateChatAutoApprove(chatId: string, autoApprove: boolean): void {
    this._chats.update(list => list?.map(c => c.id === chatId ? {...c, autoApprove} : c) ?? null);
  }

  takeStoredAutoApproveChats(): string[] {
    const key = `${AiAssistantPanelService.AUTO_APPROVE_STORAGE_KEY}_${this._autoApproveUserId ?? 'anonymous'}`;
    try {
      const ids = this.localStorage.getItem(key);
      if (ids !== null) {
        this.localStorage.removeItem(key);
      }
      return Array.isArray(ids) ? ids as string[] : [];
    } catch {
      return [];
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
      case AiAssistantViewType.ASSET:
      case AiAssistantViewType.ASSET_LIST:
        return 'ASSET';
      case AiAssistantViewType.ASSET_GROUP:
      case AiAssistantViewType.ASSET_GROUP_LIST:
        return 'ASSET_GROUP';
      case AiAssistantViewType.ENTITY_VIEW:
      case AiAssistantViewType.ENTITY_VIEW_LIST:
        return 'ENTITY_VIEW';
      case AiAssistantViewType.ENTITY_VIEW_GROUP:
      case AiAssistantViewType.ENTITY_VIEW_GROUP_LIST:
        return 'ENTITY_VIEW_GROUP';
      case AiAssistantViewType.DASHBOARD_GROUP:
      case AiAssistantViewType.DASHBOARD_GROUP_LIST:
        return 'DASHBOARD_GROUP';
      case AiAssistantViewType.DEVICE_PROFILE:
      case AiAssistantViewType.DEVICE_PROFILE_LIST:
        return 'DEVICE_PROFILE';
      case AiAssistantViewType.ASSET_PROFILE:
      case AiAssistantViewType.ASSET_PROFILE_LIST:
        return 'ASSET_PROFILE';
      case AiAssistantViewType.CUSTOMER:
      case AiAssistantViewType.CUSTOMER_LIST:
        return 'CUSTOMER';
      case AiAssistantViewType.USER:
      case AiAssistantViewType.USER_LIST:
        return 'USER';
      case AiAssistantViewType.RULE_CHAIN:
      case AiAssistantViewType.RULE_CHAIN_LIST:
        return 'RULE_CHAIN';
      case AiAssistantViewType.INTEGRATION:
      case AiAssistantViewType.INTEGRATION_LIST:
        return 'INTEGRATION';
      case AiAssistantViewType.CONVERTER:
      case AiAssistantViewType.CONVERTER_LIST:
        return 'CONVERTER';
      case AiAssistantViewType.NOTIFICATION_INBOX:
      case AiAssistantViewType.SENT_NOTIFICATION_LIST:
      case AiAssistantViewType.NOTIFICATION_TEMPLATE_LIST:
      case AiAssistantViewType.NOTIFICATION_RECIPIENT_LIST:
      case AiAssistantViewType.NOTIFICATION_RULE_LIST:
      case AiAssistantViewType.NOTIFICATION_TEMPLATE:
      case AiAssistantViewType.NOTIFICATION_RULE:
      case AiAssistantViewType.NOTIFICATION_RECIPIENT:
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

  private _viewName = signal<string | null>(null);
  readonly viewName = this._viewName.asReadonly();

  setViewName(name: string | null): void {
    this._viewName.set(name?.trim() || null);
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

  focusWhile<T>(view: AiChatView, name: string | null | undefined, until: Observable<T>): Observable<T> {
    return defer(() => {
      const previous = this._clientContext();
      if (!previous || !view.entityId?.id) {
        return until;
      }
      const previousName = this._viewName();
      const focused: AiChatClientContext = {view, timeZone: getDefaultTimezone()};
      this._clientContext.set(focused);
      this.setViewName(name ?? null);
      return until.pipe(finalize(() => {
        if (this._clientContext() === focused) {
          this._clientContext.set(previous);
          this._viewName.set(previousName);
        }
      }));
    });
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
    this._viewName.set(null);
    this._sentClientContextByChat.clear();
    this._chats.set(null);
    this._teardownTimer = setTimeout(() => {
      this._config.set(null);
    });
  }
}
