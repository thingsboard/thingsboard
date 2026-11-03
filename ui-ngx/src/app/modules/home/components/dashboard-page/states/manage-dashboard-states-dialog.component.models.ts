// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { DashboardState } from '@shared/models/dashboard.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount } from 'rxjs/operators';

export interface DashboardStateInfo extends DashboardState {
  id: string;
}

export class DashboardStatesDatasource implements DataSource<DashboardStateInfo> {

  private statesSubject = new BehaviorSubject<DashboardStateInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<DashboardStateInfo>>(emptyPageData<DashboardStateInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allStates: Observable<Array<DashboardStateInfo>>;

  constructor(private states: {[id: string]: DashboardState }) {
  }

  connect(collectionViewer: CollectionViewer): Observable<DashboardStateInfo[] | ReadonlyArray<DashboardStateInfo>> {
    return this.statesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.statesSubject.complete();
    this.pageDataSubject.complete();
  }

  loadStates(pageLink: PageLink, reload: boolean = false): Observable<PageData<DashboardStateInfo>> {
    if (reload) {
      this.allStates = null;
    }
    const result = new ReplaySubject<PageData<DashboardStateInfo>>();
    this.fetchStates(pageLink).pipe(
      catchError(() => of(emptyPageData<DashboardStateInfo>())),
    ).subscribe(
      (pageData) => {
        this.statesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchStates(pageLink: PageLink): Observable<PageData<DashboardStateInfo>> {
    return this.getAllStates().pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllStates(): Observable<Array<DashboardStateInfo>> {
    if (!this.allStates) {
      const states: DashboardStateInfo[] = [];
      for (const id of Object.keys(this.states)) {
        const state = this.states[id];
        states.push({id, ...state});
      }
      this.allStates = of(states).pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allStates;
  }

  isEmpty(): Observable<boolean> {
    return this.statesSubject.pipe(
      map((states) => !states.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

}
