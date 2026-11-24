// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  NgZone,
  SecurityContext,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { homeAgo, homeListShown } from '@home/pages/home-page/home-list.models';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { NotificationSubscriber } from '@shared/models/telemetry/telemetry.models';
import { ActionButtonLinkType, Notification, NotificationType, NotificationTypeIcons } from '@shared/models/notification.models';
import { UtilsService } from '@core/services/utils.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { StateObject } from '@core/api/widget-api.models';
import { objToBase64URI } from '@core/utils';

const LIMIT = 10;
const MINUTE = 60000;

interface NotificationHomeRow {
  id: string;
  notification: Notification;
  icon: string;
  iconColor: string | null;
  severity: string | null;
  title: string;
  message: string;
  ago: string;
  when: string;
  action: string | null;
}

@Component({
  selector: 'tb-notifications-home',
  templateUrl: './notifications-home.component.html',
  styleUrls: ['./notifications-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationsHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.NOTIFICATIONS];
  readonly inboxLink = '/notification/inbox';

  private notificationWsService = inject(NotificationWebsocketService);
  private zone = inject(NgZone);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  private translate = inject(TranslateService);
  private utils = inject(UtilsService);
  private toast = inject(ToastNotificationService);
  private host = inject(ElementRef<HTMLElement>);

  private readonly notifications = signal<Notification[] | null>(null);
  private readonly now = signal(Date.now());

  readonly total = signal(0);
  readonly expanded = signal(false);
  readonly openId = signal<string | null>(null);
  readonly markAllIcon = signal(false);

  readonly loaded = computed(() => this.notifications() !== null);

  readonly listed = computed(() => Math.max(this.total(), this.notifications()?.length ?? 0));

  readonly rows = computed<NotificationHomeRow[]>(() => {
    const now = this.now();
    const list = this.notifications() ?? [];
    return list.slice(0, homeListShown(this.listed(), this.expanded())).map(notification => this.toRow(notification, now));
  });

  private subscriber = NotificationSubscriber.createNotificationsSubscription(this.notificationWsService, this.zone, LIMIT);

  constructor() {
    const destroyRef = inject(DestroyRef);
    this.subscriber.notifications$.pipe(
      filter((value): value is Notification[] => Array.isArray(value))
    ).subscribe(list => this.notifications.set([...list]));
    this.subscriber.notificationCount$.subscribe(count => this.total.set(count));
    this.subscriber.subscribe();
    const tick = setInterval(() => this.now.set(Date.now()), MINUTE);
    let observer: ResizeObserver | null = null;
    afterNextRender(() => {
      observer = new ResizeObserver(() => this.measureHeader());
      observer.observe(this.host.nativeElement);
    });
    destroyRef.onDestroy(() => {
      clearInterval(tick);
      observer?.disconnect();
      this.subscriber.unsubscribe();
    });
  }

  toggle(row: NotificationHomeRow, event?: Event): void {
    if (event && (event.target as HTMLElement).closest('button, a')) {
      return;
    }
    this.openId.set(this.openId() === row.id ? null : row.id);
  }

  onRowKeydown(row: NotificationHomeRow, event: KeyboardEvent): void {
    if (event.target !== event.currentTarget) {
      return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.toggle(row);
    } else if (event.key === 'Escape' && this.openId() === row.id) {
      this.openId.set(null);
    }
  }

  markAsRead(row: NotificationHomeRow, event: Event): void {
    event.stopPropagation();
    NotificationSubscriber.createMarkAsReadCommand(this.notificationWsService, [row.id]).subscribe();
    this.notify(this.translate.instant('home.notifications.marked-read'));
  }

  markAllAsRead(): void {
    const count = this.total();
    NotificationSubscriber.createMarkAllAsReadCommand(this.notificationWsService).subscribe();
    this.openId.set(null);
    this.notify(this.translate.instant(count === 1 ? 'home.notifications.marked-read' : 'home.notifications.marked-all-read', {count}));
  }

  runAction(row: NotificationHomeRow, event: Event): void {
    event.stopPropagation();
    const config = row.notification.additionalConfig.actionButtonConfig;
    let link: string;
    if (config.linkType === ActionButtonLinkType.DASHBOARD) {
      let state: string = null;
      if (config.dashboardState || config.setEntityIdInState) {
        const stateObject: StateObject = {
          params: config.setEntityIdInState ? {entityId: row.notification.info?.stateEntityId ?? null} : {}
        };
        if (config.dashboardState) {
          stateObject.id = config.dashboardState;
        }
        state = objToBase64URI([stateObject]);
      }
      link = `/dashboards/${config.dashboardId}` + (state ? `?state=${state}` : '');
    } else {
      link = config.link;
    }
    if (link.startsWith('/')) {
      this.router.navigateByUrl(this.router.parseUrl(link));
    } else {
      window.open(link, '_blank');
    }
  }

  private measureHeader(): void {
    const element: HTMLElement = this.host.nativeElement;
    const title = element.querySelector('.tb-home-card-title-group');
    const button = element.querySelector('.tb-notifications-mark-all');
    if (!title || !button) {
      return;
    }
    this.zone.run(() => this.markAllIcon.set(false));
    requestAnimationFrame(() => {
      const wrapped = button.getBoundingClientRect().top >= title.getBoundingClientRect().bottom - 4;
      this.zone.run(() => this.markAllIcon.set(wrapped));
    });
  }

  private toRow(notification: Notification, now: number): NotificationHomeRow {
    const createdTime = notification.createdTime ?? now;
    const ownIcon = notification.additionalConfig?.icon?.enabled ? notification.additionalConfig.icon : null;
    const alarm = notification.type === NotificationType.ALARM;
    const button = notification.additionalConfig?.actionButtonConfig;
    return {
      id: notification.id.id,
      notification,
      icon: ownIcon?.icon ?? NotificationTypeIcons.get(notification.type) ?? 'notifications',
      iconColor: ownIcon?.color ?? this.iconColor(notification.type),
      severity: alarm ? notification.info?.alarmSeverity ?? null : null,
      title: this.sanitizer.sanitize(SecurityContext.HTML, this.utils.customTranslation(notification.subject)),
      message: this.sanitizer.sanitize(SecurityContext.HTML, this.utils.customTranslation(notification.text)),
      ago: homeAgo(this.translate, now - createdTime),
      when: this.exactTime(createdTime),
      action: button?.enabled ? this.utils.customTranslation(button.text) : null
    };
  }

  private exactTime(ts: number): string {
    const date = new Date(ts);
    const month = date.toLocaleString(this.translate.currentLang?.replace('_', '-') || 'en-US', {month: 'short'});
    const pad = (value: number) => String(value).padStart(2, '0');
    return `${pad(date.getDate())} ${month}, ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private iconColor(type: NotificationType): string | null {
    switch (type) {
      case NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT:
        return '#D12730';
      case NotificationType.ENTITIES_LIMIT_INCREASE_REQUEST:
      case NotificationType.ADDON_ACCESS_REQUEST:
      case NotificationType.ADDON_ACCESS_ERROR:
      case NotificationType.PLAN_UPGRADE_REQUEST:
        return 'var(--tb-primary-500)';
      default:
        return null;
    }
  }

  private notify(message: string): void {
    this.toast.dispatchNotification({
      message,
      type: 'info',
      duration: 4000,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast'
    });
  }
}
