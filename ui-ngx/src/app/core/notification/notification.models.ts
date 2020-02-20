///
/// Copyright © 2016-2020 The Thingsboard Authors
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


export interface NotificationState {
  notification: NotificationMessage;
  hideNotification: HideNotification;
}

export declare type NotificationType = 'info' | 'success' | 'error';
export declare type NotificationHorizontalPosition = 'start' | 'center' | 'end' | 'left' | 'right';
export declare type NotificationVerticalPosition = 'top' | 'bottom';

export class NotificationMessage {
  message: string;
  type: NotificationType;
  target?: string;
  duration?: number;
  forceDismiss?: boolean;
  horizontalPosition?: NotificationHorizontalPosition;
  verticalPosition?: NotificationVerticalPosition;
  panelClass?: string | string[];
}

export class HideNotification {
  target?: string;
}
