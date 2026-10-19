// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
