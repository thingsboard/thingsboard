// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import {
  ALL_RELATIONS_DIRECTION,
  EntityRelationInfo,
  EntitySearchDirection,
  relationDirectionTranslations,
  RelationsTableDirection
} from '@shared/models/relation.models';
import { BehaviorSubject, forkJoin, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { defaultPageLinkSearchFunction, PageLink, PageLinkSearchFunction } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { EntityId, entityIdEquals } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';

const relationSearchFunction: PageLinkSearchFunction<EntityRelationInfo> =
  ({direction, directionName, ...relation}, textSearch) => defaultPageLinkSearchFunction()(relation, textSearch);

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

  loadRelations(direction: RelationsTableDirection, entityId: EntityId,
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

  fetchRelations(direction: RelationsTableDirection, entityId: EntityId,
                 pageLink: PageLink): Observable<PageData<EntityRelationInfo>> {
    return this.getAllRelations(direction, entityId).pipe(
      map((data) => pageLink.filterData(data, relationSearchFunction))
    );
  }

  getAllRelations(direction: RelationsTableDirection, entityId: EntityId): Observable<Array<EntityRelationInfo>> {
    if (!this.allRelations) {
      let relationsObservable: Observable<Array<EntityRelationInfo>>;
      switch (direction) {
        case EntitySearchDirection.FROM:
        case EntitySearchDirection.TO:
          relationsObservable = this.getRelationsByDirection(direction, entityId);
          break;
        case ALL_RELATIONS_DIRECTION:
          relationsObservable = forkJoin([
            this.getRelationsByDirection(EntitySearchDirection.FROM, entityId).pipe(catchError(() => of([]))),
            this.getRelationsByDirection(EntitySearchDirection.TO, entityId).pipe(catchError(() => of([])))
          ]).pipe(
            // A relation from the entity to itself is returned in both directions, keep only the outbound one
            map(([fromRelations, toRelations]) => [
              ...fromRelations,
              ...toRelations.filter(relation => !entityIdEquals(relation.from, entityId))
            ])
          );
          break;
      }
      this.allRelations = relationsObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allRelations;
  }

  private getRelationsByDirection(direction: EntitySearchDirection, entityId: EntityId): Observable<Array<EntityRelationInfo>> {
    const relationsObservable = direction === EntitySearchDirection.FROM ?
      this.entityRelationService.findInfoByFrom(entityId) : this.entityRelationService.findInfoByTo(entityId);
    return relationsObservable.pipe(
      map(relations => relations.map(relation => {
        const entity = direction === EntitySearchDirection.FROM ? relation.to : relation.from;
        const entityTypeName = this.translate.instant(entityTypeTranslations.get(entity.entityType as EntityType).type);
        return {
          ...relation,
          direction,
          directionName: this.translate.instant(relationDirectionTranslations.get(direction)),
          entityTypeName,
          entityName: direction === EntitySearchDirection.FROM ? relation.toName : relation.fromName,
          entityURL: getEntityDetailsPageURL(entity.id, entity.entityType as EntityType)
        };
      }))
    );
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
