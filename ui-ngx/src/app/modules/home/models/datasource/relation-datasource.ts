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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { EntityRelationInfo, EntitySearchDirection } from '@shared/models/relation.models';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { EntityId } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { entityTypeTranslations } from '@shared/models/entity-type.models';

export class RelationsDatasource implements DataSource<EntityRelationInfo> {

  private relationsSubject = new BehaviorSubject<EntityRelationInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<EntityRelationInfo>>(emptyPageData<EntityRelationInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<EntityRelationInfo>(true, []);

  private allRelations: Observable<Array<EntityRelationInfo>>;

  constructor(private entityRelationService: EntityRelationService,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<EntityRelationInfo[] | ReadonlyArray<EntityRelationInfo>> {
    return this.relationsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.relationsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadRelations(direction: EntitySearchDirection, entityId: EntityId,
                pageLink: PageLink, reload: boolean = false): Observable<PageData<EntityRelationInfo>> {
    if (reload) {
      this.allRelations = null;
    }
    const result = new ReplaySubject<PageData<EntityRelationInfo>>();
    this.fetchRelations(direction, entityId, pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<EntityRelationInfo>())),
    ).subscribe(
      (pageData) => {
        this.relationsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchRelations(direction: EntitySearchDirection, entityId: EntityId,
                 pageLink: PageLink): Observable<PageData<EntityRelationInfo>> {
    return this.getAllRelations(direction, entityId).pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllRelations(direction: EntitySearchDirection, entityId: EntityId): Observable<Array<EntityRelationInfo>> {
    if (!this.allRelations) {
      let relationsObservable: Observable<Array<EntityRelationInfo>>;
      switch (direction) {
        case EntitySearchDirection.FROM:
          relationsObservable = this.entityRelationService.findInfoByFrom(entityId);
          break;
        case EntitySearchDirection.TO:
          relationsObservable = this.entityRelationService.findInfoByTo(entityId);
          break;
      }
      this.allRelations = relationsObservable.pipe(
        map(relations => {
          relations.forEach(relation => {
            if (direction === EntitySearchDirection.FROM) {
              relation.toEntityTypeName = this.translate.instant(entityTypeTranslations.get(relation.to.entityType).type);
            } else {
              relation.fromEntityTypeName = this.translate.instant(entityTypeTranslations.get(relation.from.entityType).type);
            }
          });
          return relations;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.allRelations;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.relationsSubject.pipe(
      map((relations) => numSelected === relations.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.relationsSubject.pipe(
      map((relations) => !relations.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.relationsSubject.pipe(
      tap((relations) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === relations.length) {
          this.selection.clear();
        } else {
          relations.forEach(row => {
            this.selection.select(row);
          });
        }
      }),
      take(1)
    ).subscribe();
  }
}
