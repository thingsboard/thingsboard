// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Action } from '@ngrx/store';
import { HideNotification, NotificationMessage } from '@app/core/notification/notification.models';

export enum NotificationActionTypes {
  SHOW_NOTIFICATION = '[Notification] Show',
  HIDE_NOTIFICATION = '[Notification] Hide'
}

export class ActionNotificationShow implements Action {
  readonly type = NotificationActionTypes.SHOW_NOTIFICATION;

  constructor(readonly notification: NotificationMessage ) {}
}

export class ActionNotificationHide implements Action {
  readonly type = NotificationActionTypes.HIDE_NOTIFICATION;

  constructor(readonly hideNotification: HideNotification ) {}
}

export type NotificationActions =
  | ActionNotificationShow | ActionNotificationHide;
