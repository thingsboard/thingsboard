///
/// Copyright © 2016-2022 The Thingsboard Authors
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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { Observable } from 'rxjs';
import { distinctUntilChanged, publishReplay, refCount, tap } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ShowNotificationPopoverComponent } from '@home/components/notification/show-notification-popover.component';
import { NotificationSubscriber } from '@shared/models/notification-ws.models';

@Component({
  selector: 'tb-notification-bell',
  templateUrl: './notification-bell.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationBellComponent implements OnInit {

  private notificationSubscriber: NotificationSubscriber;
  count$: Observable<number>;

  constructor(
    private notificationWsService: NotificationWebsocketService,
    private zone: NgZone,
    private cd: ChangeDetectorRef,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.notificationSubscriber = NotificationSubscriber.createNotificationCountSubscription(
      this.notificationWsService, this.zone);
    this.notificationSubscriber.subscribe();
    this.count$ = this.notificationSubscriber.notificationCount$.pipe(
      distinctUntilChanged(),
      publishReplay(1),
      refCount(),
      tap(() => setTimeout(() => this.cd.markForCheck())),
    );
  }

  showNotification($event: Event, createVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = createVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const showNotificationPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ShowNotificationPopoverComponent, 'bottom', true, null,
        {
          onClose: () => {
            showNotificationPopover.hide();
          }
        },
        {maxHeight: '100vh', height: '100%', padding: '10px'},
        {width: '400px', minWidth: '100%', maxWidth: '100%'},
        {height: '100%', flexDirection: 'column', boxSizing: 'border-box', display: 'flex'}, false);
      showNotificationPopover.tbComponentRef.instance.popoverComponent = showNotificationPopover;
    }
  }
}
