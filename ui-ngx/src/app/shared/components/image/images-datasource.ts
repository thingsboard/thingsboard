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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { ImageResourceInfo, ResourceSubType } from '@shared/models/resource.models';
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { ImageService } from '@core/http/image.service';
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, take, tap } from 'rxjs/operators';

export class ImagesDatasource implements DataSource<ImageResourceInfo> {
  private entitiesSubject: Subject<ImageResourceInfo[]>;
  private readonly pageDataSubject: Subject<PageData<ImageResourceInfo>>;

  public pageData$: Observable<PageData<ImageResourceInfo>>;

  public selection = new SelectionModel<ImageResourceInfo>(true, []);

  public dataLoading = true;

  constructor(private imageService: ImageService,
              private images: ImageResourceInfo[],
              private selectionEnabledFunction: EntityBooleanFunction<ImageResourceInfo>) {
    if (this.images && this.images.length) {
      this.entitiesSubject = new BehaviorSubject<ImageResourceInfo[]>(this.images);
    } else {
      this.entitiesSubject = new BehaviorSubject<ImageResourceInfo[]>([]);
      this.pageDataSubject = new BehaviorSubject<PageData<ImageResourceInfo>>(emptyPageData<ImageResourceInfo>());
      this.pageData$ = this.pageDataSubject.asObservable();
    }
  }

  connect(collectionViewer: CollectionViewer):
    Observable<ImageResourceInfo[] | ReadonlyArray<ImageResourceInfo>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    if (this.pageDataSubject) {
      this.pageDataSubject.complete();
    }
  }

  reset() {
    this.entitiesSubject.next([]);
    if (this.pageDataSubject) {
      this.pageDataSubject.next(emptyPageData<ImageResourceInfo>());
    }
  }

  loadEntities(pageLink: PageLink, imageSubType: ResourceSubType, includeSystemImages = false): Observable<PageData<ImageResourceInfo>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<ImageResourceInfo>>();
    this.fetchEntities(pageLink, imageSubType, includeSystemImages).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<ImageResourceInfo>())),
    ).subscribe(
      (pageData) => {
        this.entitiesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoading = false;
      }
    );
    return result;
  }

  fetchEntities(pageLink: PageLink, imageSubType: ResourceSubType, includeSystemImages = false): Observable<PageData<ImageResourceInfo>> {
    return this.imageService.getImages(pageLink, includeSystemImages, imageSubType);
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map((entities) => numSelected === entities.length)
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

  private selectableEntitiesCount(entities: Array<ImageResourceInfo>): number {
    return entities.filter((entity) => this.selectionEnabledFunction(entity)).length;
  }
}
