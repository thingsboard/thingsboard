///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ChangeDetectorRef,
  Component,
  Injector,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  StaticProvider,
  TemplateRef,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { WidgetAction, WidgetContext } from '@home/models/widget-component.models';
import { isDefined } from '@core/utils';
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { BehaviorSubject, fromEvent, Observable, ReplaySubject, Subscription } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  unreadNotificationDefaultSettings,
  UnreadNotificationWidgetSettings
} from '@home/components/widget/lib/cards/unread-notification-widget.models';
import { Notification, NotificationRequest, NotificationType } from '@shared/models/notification.models';
import { NotificationSubscriber } from '@shared/models/telemetry/telemetry.models';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { distinctUntilChanged, map, share, skip, take, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  NOTIFICATION_TYPE_FILTER_PANEL_DATA,
  NotificationTypeFilterPanelComponent
} from '@home/components/widget/lib/cards/notification-type-filter-panel.component';
import { selectUserDetails } from '@core/auth/auth.selectors';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-unread-notification-widget',
  templateUrl: './unread-notification-widget.component.html',
  styleUrls: ['unread-notification-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class UnreadNotificationWidgetComponent implements OnInit, OnDestroy {

  settings: UnreadNotificationWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showCounter = true;
  counterValueStyle: ComponentStyle;
  counterBackground: string;

  notifications: Notification[];
  loadNotification = false;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private counterValue: BehaviorSubject<number>  = new BehaviorSubject(0);

  count$ = this.counterValue.asObservable().pipe(
    distinctUntilChanged(),
    map((value) => value >= 100 ? '99+' : value),
    tap(() => Promise.resolve().then(() => this.cd.markForCheck())),
    share({
      connector: () => new ReplaySubject(1)
    })
  );


  private notificationTypes: Array<NotificationType> = [];

  private notificationSubscriber: NotificationSubscriber;
  private notificationCountSubscriber: Subscription;
  private notification: Subscription;

  private contentResize$: ResizeObserver;

  private defaultDashboardFullscreen = false;

  private viewAllAction: WidgetAction = {
    name: 'widgets.notification.button-view-all',
    show: !this.defaultDashboardFullscreen,
    icon: 'open_in_new',
    onAction: ($event) => {
      this.viewAll($event);
    }
  };

  private filterAction: WidgetAction = {
    name: 'widgets.notification.button-filter',
    show: true,
    icon: 'filter_list',
    onAction: ($event) => {
      this.editNotificationTypeFilter($event);
    }
  };

  private markAsReadAction: WidgetAction = {
    name: 'widgets.notification.button-mark-read',
    show: true,
    icon: 'done_all',
    onAction: ($event) => {
      this.markAsAllRead($event);
    }
  };

  constructor(private store: Store<AppState>,
              private imagePipe: ImagePipe,
              private notificationWsService: NotificationWebsocketService,
              private sanitizer: DomSanitizer,
              private router: Router,
              private zone: NgZone,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.unreadNotificationWidget = this;
    this.settings = {...unreadNotificationDefaultSettings, ...this.ctx.settings};

    this.showCounter = this.settings.showCounter;
    this.counterValueStyle = textStyle(this.settings.counterValueFont);
    this.counterValueStyle.color = this.settings.counterValueColor;
    this.counterBackground = this.settings.counterColor;

    this.ctx.widgetActions = [this.viewAllAction, this.filterAction, this.markAsReadAction];

    this.viewAllAction.show = isDefined(this.settings.enableViewAll) ? this.settings.enableViewAll : true;
    if (this.viewAllAction.show) {
      this.store.pipe(select(selectUserDetails), take(1)).subscribe(
        user => this.viewAllAction.show = !user.additionalInfo?.defaultDashboardFullscreen
      );
    }
    this.filterAction.show = isDefined(this.settings.enableFilter) ? this.settings.enableFilter : true;
    this.markAsReadAction.show = isDefined(this.settings.enableMarkAsRead) ? this.settings.enableMarkAsRead : true;

    this.initSubscription();

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }


  ngOnDestroy() {
    if (this.contentResize$) {
      this.contentResize$.disconnect();
    }
    this.unsubscribeSubscription();
  }

  private initSubscription() {
    this.notificationSubscriber = NotificationSubscriber.createNotificationsSubscription(
      this.notificationWsService, this.zone, this.settings.maxNotificationDisplay, this.notificationTypes);
    this.notification = this.notificationSubscriber.notifications$.subscribe(value => {
      if (Array.isArray(value)) {
        this.loadNotification = true;
        this.notifications = value;
        this.cd.markForCheck();
      }
    });
    this.notificationCountSubscriber = this.notificationSubscriber.notificationCount$.pipe(
      skip(1),
    ).subscribe(value => this.counterValue.next(value));
    this.notificationSubscriber.subscribe();
  }

  private unsubscribeSubscription() {
    this.notificationSubscriber.unsubscribe();
    this.notificationCountSubscriber.unsubscribe();
    this.notification.unsubscribe();
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  markAsRead(id: string) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      const cmd = NotificationSubscriber.createMarkAsReadCommand(this.notificationWsService, [id]);
      cmd.subscribe();
    }
  }

  markAsAllRead($event: Event) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      if ($event) {
        $event.stopPropagation();
      }
      const cmd = NotificationSubscriber.createMarkAllAsReadCommand(this.notificationWsService);
      cmd.subscribe();
    }
  }

  viewAll($event: Event) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      if ($event) {
        $event.stopPropagation();
      }
      this.router.navigateByUrl(this.router.parseUrl('/notification/inbox')).then(() => {});
    }
  }

  trackById(index: number, item: NotificationRequest): string {
    return item.id.id;
  }

  private editNotificationTypeFilter($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig({
      panelClass: 'tb-panel-container',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      height: 'fit-content',
      maxHeight: '75vh',
      width: '100%',
      maxWidth: 700
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(target as HTMLElement)
      .withPositions(DEFAULT_OVERLAY_POSITIONS);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const providers: StaticProvider[] = [
      {
        provide: NOTIFICATION_TYPE_FILTER_PANEL_DATA,
        useValue: {
          notificationTypes: this.notificationTypes,
          notificationTypesUpdated: (notificationTypes: Array<NotificationType>) => {
            this.notificationTypes = notificationTypes;
            this.unsubscribeSubscription();
            this.initSubscription();
          }
        }
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];

    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(NotificationTypeFilterPanelComponent,
      this.viewContainerRef, injector));

    const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
      overlayRef.updatePosition();
    });
    componentRef.onDestroy(() => {
      resizeWindows$.unsubscribe();
    });

    this.ctx.detectChanges();
  }

}
