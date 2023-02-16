import { NgZone } from '@angular/core';
import { WebsocketCmd } from '@shared/models/telemetry/telemetry.models';
import { Subject } from 'rxjs';

export interface WsService<T extends WsSubscriber> {
  subscribe(subscriber: T);
  update(subscriber: T);
  unsubscribe(subscriber: T);
}

export abstract class CmdWrapper {
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
