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

import { NgZone } from '@angular/core';
import { WebsocketCmd } from '@shared/models/telemetry/telemetry.models';
import { Subject } from 'rxjs';

export interface WsService<T extends WsSubscriber> {
  subscribe(subscriber: T);
  update(subscriber: T);
  unsubscribe(subscriber: T);
}

export abstract class CmdWrapper {
  abstract setAuth(token: string);
  abstract hasCommands(): boolean;
  abstract clear(): void;
  abstract preparePublishCommands(maxCommands: number): CmdWrapper;

  [key: string]: WebsocketCmd | any;
}

export abstract class WsSubscriber {

  protected reconnectSubject = new Subject<void>();

  subscriptionCommands: Array<WebsocketCmd>;

  reconnect$ = this.reconnectSubject.asObservable();

  protected constructor(protected wsService: WsService<WsSubscriber>, protected zone?: NgZone) {
    this.subscriptionCommands = [];
  }

  public subscribe() {
    this.wsService.subscribe(this);
  }

  public update() {
    this.wsService.update(this);
  }

  public unsubscribe() {
    this.wsService.unsubscribe(this);
    this.complete();
  }

  public complete() {
    this.reconnectSubject.complete();
  }

  public onReconnected() {
    this.reconnectSubject.next();
  }
}
