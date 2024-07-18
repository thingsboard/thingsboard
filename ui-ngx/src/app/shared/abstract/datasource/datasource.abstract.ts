///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export abstract class TbDatasource<DataType> implements DataSource<DataType> {

  protected dataSubject = new BehaviorSubject<Array<DataType>>([]);

  connect(): Observable<Array<DataType>> {
    return this.dataSubject.asObservable();
  }

  disconnect(): void {
    this.dataSubject.complete();
  }

  loadData(data: Array<DataType>): void {
    this.dataSubject.next(data);
  }

  isEmpty(): Observable<boolean> {
    return this.dataSubject.pipe(
      map((data: DataType[]) => !data.length)
    );
  }

  total(): Observable<number> {
    return this.dataSubject.pipe(
      map((data: DataType[]) => data.length)
    );
  }
}
