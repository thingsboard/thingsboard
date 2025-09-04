///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { HideNotification, NotificationMessage } from '@app/core/notification/notification.models';
import { Observable, Subject } from 'rxjs';


@Injectable(
  {
    providedIn: 'root'
  }
)
export class ToastNotificationService {

  private notificationSubject: Subject<NotificationMessage> = new Subject();

  private hideNotificationSubject: Subject<HideNotification> = new Subject();

  constructor(
  ) {
  }

  dispatchNotification(notification: NotificationMessage) {
    this.notificationSubject.next(notification);
  }

  hideNotification(hideNotification: HideNotification) {
    this.hideNotificationSubject.next(hideNotification);
  }

  getNotification(): Observable<NotificationMessage> {
    return this.notificationSubject.asObservable();
  }

  getHideNotification(): Observable<HideNotification> {
    return this.hideNotificationSubject.asObservable();
  }
}
