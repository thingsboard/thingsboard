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
