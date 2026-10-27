// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
