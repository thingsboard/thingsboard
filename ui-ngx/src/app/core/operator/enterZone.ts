// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { MonoTypeOperatorFunction, Observable, Operator, Subscriber } from 'rxjs';

export type EnterZoneSignature<T> = (zone: { run: (fn: any) => any }) => Observable<T>;

export function enterZone<T>(zone: { run: (fn: any) => any }): MonoTypeOperatorFunction<T> {
  return (source: Observable<T>) => {
    return source.lift(new EnterZoneOperator(zone));
  };
}

export class EnterZoneOperator<T> implements Operator<T, T> {
  constructor(private zone: { run: (fn: any) => any }) { }

  call(subscriber: Subscriber<T>, source: any): any {
    return source._subscribe(new EnterZoneSubscriber(subscriber, this.zone));
  }
}

class EnterZoneSubscriber<T> extends Subscriber<T> {
  constructor(destination: Subscriber<T>, private zone: { run: (fn: any) => any }) {
    super(destination);
  }

  protected _next(value: T) {
    this.zone.run(() => this.destination.next(value));
  }
}
