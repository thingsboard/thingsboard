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

import { Injectable } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { BroadcastEvent, BroadcastListener, BroadcastMessage } from '@core/services/broadcast.models';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class BroadcastService {

  private broadcastSubject: Subject<BroadcastMessage> = new Subject();

  broadcast(name: string, ...args: Array<any>) {
    const message = {
      name,
      args
    } as BroadcastMessage;
    this.broadcastSubject.next(message);
  }

  on(name: string, listener: BroadcastListener): Subscription {
    return this.broadcastSubject.asObservable().pipe(
      filter((message) => message.name === name)
    ).subscribe(
      (message) => {
        const event = {
          name: message.name
        } as BroadcastEvent;
        listener(event, message.args);
      }
    );
  }

}
