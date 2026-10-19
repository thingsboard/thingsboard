// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export abstract class TbTableDatasource<T> implements DataSource<T> {

  protected dataSubject = new BehaviorSubject<Array<T>>([]);

  connect(): Observable<Array<T>> {
    return this.dataSubject.asObservable();
  }

  disconnect(): void {
    this.dataSubject.complete();
  }

  loadData(data: Array<T>): void {
    this.dataSubject.next(data);
  }

  isEmpty(): Observable<boolean> {
    return this.dataSubject.pipe(
      map((data: T[]) => !data.length)
    );
  }

  total(): Observable<number> {
    return this.dataSubject.pipe(
      map((data: T[]) => data.length)
    );
  }
}
