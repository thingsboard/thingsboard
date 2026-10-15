// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, take, tap } from 'rxjs/operators';
import { SecretStorage } from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';

export class SecretsDatasource implements DataSource<SecretStorage> {
  private entitiesSubject: Subject<SecretStorage[]>;
  private readonly pageDataSubject: Subject<PageData<SecretStorage>>;

  public pageData$: Observable<PageData<SecretStorage>>;

  public selection = new SelectionModel<SecretStorage>(true, []);

  public dataLoading = true;

  constructor(private secretStorageService: SecretStorageService,
              private secrets: SecretStorage[],
              private selectionEnabledFunction: EntityBooleanFunction<SecretStorage>) {
    if (this.secrets && this.secrets.length) {
      this.entitiesSubject = new BehaviorSubject<SecretStorage[]>(this.secrets);
    } else {
      this.entitiesSubject = new BehaviorSubject<SecretStorage[]>([]);
      this.pageDataSubject = new BehaviorSubject<PageData<SecretStorage>>(emptyPageData<SecretStorage>());
      this.pageData$ = this.pageDataSubject.asObservable();
    }
  }

  connect(collectionViewer: CollectionViewer):
    Observable<SecretStorage[] | ReadonlyArray<SecretStorage>> {
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
      this.pageDataSubject.next(emptyPageData<SecretStorage>());
    }
  }

  loadEntities(pageLink: PageLink): Observable<PageData<SecretStorage>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<SecretStorage>>();
    this.fetchEntities(pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<SecretStorage>())),
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

  fetchEntities(pageLink: PageLink): Observable<PageData<SecretStorage>> {
    return this.secretStorageService.getSecrets(pageLink);
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

  private selectableEntitiesCount(entities: Array<SecretStorage>): number {
    return entities.filter((entity) => this.selectionEnabledFunction(entity)).length;
  }
}
