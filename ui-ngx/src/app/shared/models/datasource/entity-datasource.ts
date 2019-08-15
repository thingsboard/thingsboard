///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { EntityId } from '@shared/models/id/entity-id';
import { PageLink } from '@shared/models/page/page-link';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { BaseData, HasId } from '@shared/models/base-data';
import { CollectionViewer, DataSource } from '@angular/cdk/typings/collections';
import { catchError, map, take, tap } from 'rxjs/operators';
import { SelectionModel } from '@angular/cdk/collections';
import {EntityBooleanFunction} from '@shared/components/entity/entities-table-config.models';

export type EntitiesFetchFunction<T extends BaseData<HasId>, P extends PageLink> = (pageLink: P) => Observable<PageData<T>>;

export class EntitiesDataSource<T extends BaseData<HasId>, P extends PageLink = PageLink> implements DataSource<T> {

  private entitiesSubject = new BehaviorSubject<T[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<T>>(emptyPageData<T>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<T>(true, []);

  public currentEntity: T = null;

  constructor(private fetchFunction: EntitiesFetchFunction<T, P>,
              private selectionEnabledFunction: EntityBooleanFunction<T>,
              private dataLoadedFunction: () => void) {}

  connect(collectionViewer: CollectionViewer): Observable<T[] | ReadonlyArray<T>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  loadEntities(pageLink: P): Observable<PageData<T>> {
    const result = new ReplaySubject<PageData<T>>();
    this.fetchFunction(pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<T>())),
    ).subscribe(
      (pageData) => {
        this.entitiesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoadedFunction();
      }
    );
    return result;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map((entities) => numSelected === this.selectableEntitiesCount(entities))
    );
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
      map((entities) => !entities.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  toggleCurrentEntity(entity: T): boolean {
    if (this.currentEntity !== entity) {
      this.currentEntity = entity;
      return true;
    } else {
      return false;
    }
  }

  isCurrentEntity(entity: T): boolean {
    return (this.currentEntity && entity && this.currentEntity.id && entity.id) &&
      (this.currentEntity.id.id === entity.id.id);
  }

  masterToggle() {
    this.entitiesSubject.pipe(
      tap((entities) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === this.selectableEntitiesCount(entities)) {
          this.selection.clear();
        } else {
          entities.forEach(row => {
            if (this.selectionEnabledFunction(row)) {
              this.selection.select(row);
            }
          });
        }
      }),
      take(1)
    ).subscribe();
  }

  private selectableEntitiesCount(entities: Array<T>): number {
    return entities.filter((entity) => this.selectionEnabledFunction(entity)).length;
  }
}
