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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  AbstractUserDashboardInfo,
  LastVisitedDashboardInfo,
  StarredDashboardInfo,
  UserDashboardAction,
  UserDashboardsInfo
} from '@shared/models/user-settings.models';
import { UserSettingsService } from '@core/http/user-settings.service';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { MAX_SAFE_PAGE_SIZE, PageLink } from '@shared/models/page/page-link';
import { map, share } from 'rxjs/operators';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatSort } from '@angular/material/sort';
import { DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardAutocompleteComponent } from '@shared/components/dashboard-autocomplete.component';

@Component({
  selector: 'tb-recent-dashboards-widget',
  templateUrl: './recent-dashboards-widget.component.html',
  styleUrls: ['./home-page-widget.scss', './recent-dashboards-widget.component.scss']
})
export class RecentDashboardsWidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @ViewChildren(MatSort) lastVisitedDashboardsSort: QueryList<MatSort>;

  @ViewChild('starDashboardAutocomplete', {static: false})
  starDashboardAutocomplete: DashboardAutocompleteComponent;

  authority = Authority;

  userDashboardsInfo: UserDashboardsInfo;
  authUser = getCurrentAuthUser(this.store);

  toggleValue: 'last' | 'starred' = 'last';

  lastVisitedDashboardsColumns = ['starred', 'title', 'lastVisited'];
  lastVisitedDashboardsDataSource: LastVisitedDashboardsDataSource;
  lastVisitedDashboardsPageLink: PageLink;

  starredDashboardValue = null;
  hasDashboardsAccess = true;

  dirty = false;
  public customerId: string;
  private isFullscreenMode = getCurrentAuthState(this.store).forceFullscreen;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private userSettingService: UserSettingsService) {
    super(store);
  }

  ngOnInit() {
    if (this.authUser.authority === Authority.CUSTOMER_USER) {
      this.customerId = this.authUser.customerId;
    }
    this.hasDashboardsAccess = [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER].includes(this.authUser.authority);
    if (this.hasDashboardsAccess) {
      this.reload();
    }
  }

  reload() {
    this.userDashboardsInfo = null;
    this.cd.markForCheck();
    (this.authUser.authority !== Authority.SYS_ADMIN ?
      this.userSettingService.getUserDashboardsInfo() : of({last: [], starred: []})).subscribe(
      (userDashboardsInfo) => {
        this.userDashboardsInfo = userDashboardsInfo;
        for (const starredDashboard of this.userDashboardsInfo?.starred) {
          starredDashboard.starred = true;
        }
        this.userDashboardsInfo?.starred.sort((a, b) => a.starredAt - b.starredAt);
        if (this.hasLastVisitedDashboards()) {
          this.initLastVisitedDashboardsDataSource();
        }
        this.cd.markForCheck();
      }
    );
  }

  public createDashboardUrl(id: string): string {
    const baseUrl = this.isFullscreenMode ? '/dashboard/' : '/dashboards/';
    return baseUrl + id;
  }

  toggleValueChange(value: 'last' | 'starred') {
    this.toggleValue = value;
    if (this.dirty) {
      this.dirty = false;
      this.reload();
    } else {
      if (value === 'last' && this.hasLastVisitedDashboards()) {
        this.initLastVisitedDashboardsDataSource();
      }
    }
  }

  private initLastVisitedDashboardsDataSource() {
    this.lastVisitedDashboardsDataSource = new LastVisitedDashboardsDataSource(this.userDashboardsInfo.last);
    const sortOrder: SortOrder = {
      property: 'lastVisited',
      direction: Direction.DESC
    };
    this.lastVisitedDashboardsPageLink = new PageLink(MAX_SAFE_PAGE_SIZE, 0, null, sortOrder);
    this.lastVisitedDashboardsDataSource.loadData(this.lastVisitedDashboardsPageLink);
  }

  ngAfterViewInit() {
    this.lastVisitedDashboardsSort.changes.subscribe(() => {
      if (this.lastVisitedDashboardsSort.length) {
        this.lastVisitedDashboardsSort.get(0).sortChange.subscribe(() =>
          this.updateLastVisitedDashboardsData(this.lastVisitedDashboardsSort.get(0)));
      }
    });
  }

  updateLastVisitedDashboardsData(sort: MatSort) {
    this.lastVisitedDashboardsPageLink.sortOrder.property = sort.active;
    this.lastVisitedDashboardsPageLink.sortOrder.direction = Direction[sort.direction.toUpperCase()];
    this.lastVisitedDashboardsDataSource.loadData(this.lastVisitedDashboardsPageLink);
  }

  hasLastVisitedDashboards(): boolean {
    return !!(this.userDashboardsInfo && this.userDashboardsInfo.last && this.userDashboardsInfo.last.length);
  }

  toggleDashboardStar(dashboard: AbstractUserDashboardInfo): void {
    const action: UserDashboardAction = dashboard.starred ? UserDashboardAction.UNSTAR : UserDashboardAction.STAR;
    dashboard.starred = !dashboard.starred;
    this.userSettingService.reportUserDashboardAction(dashboard.id, action, {ignoreLoading: true}).subscribe();
    this.dirty = true;
    if (this.toggleValue === 'starred') {
      const index = this.userDashboardsInfo.starred.findIndex((d) => d.id === dashboard.id);
      if (index > -1) {
        this.userDashboardsInfo.starred.splice(index, 1);
        this.cd.markForCheck();
      }
    }
  }

  onStarDashboard(dashboard: DashboardInfo) {
    if (dashboard) {
      this.starDashboardAutocomplete.clear();
      const index = this.userDashboardsInfo.starred.findIndex((d) => d.id === dashboard.id.id);
      if (index === -1) {
        const starredDashboard: StarredDashboardInfo = {
          starredAt: Date.now(),
          id: dashboard.id.id,
          starred: true,
          title: dashboard.title
        };
        this.userDashboardsInfo.starred.push(starredDashboard);
        this.userSettingService.reportUserDashboardAction(dashboard.id.id, UserDashboardAction.STAR, {ignoreLoading: true}).subscribe();
      }
      this.cd.markForCheck();
    }
  }

}

export class LastVisitedDashboardsDataSource implements DataSource<LastVisitedDashboardInfo> {

  private lastVisitedDashboardsSubject = new BehaviorSubject<LastVisitedDashboardInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<LastVisitedDashboardInfo>>(emptyPageData<LastVisitedDashboardInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  constructor(private lastVisitedDashboards: Array<LastVisitedDashboardInfo>) {
  }

  connect(collectionViewer: CollectionViewer): Observable<LastVisitedDashboardInfo[] | ReadonlyArray<LastVisitedDashboardInfo>> {
    return this.lastVisitedDashboardsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.lastVisitedDashboardsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadData(pageLink: PageLink): void {
    const result = pageLink.filterData(this.lastVisitedDashboards);
    this.lastVisitedDashboardsSubject.next(result.data);
    this.pageDataSubject.next(result);
  }

  isEmpty(): Observable<boolean> {
    return this.lastVisitedDashboardsSubject.pipe(
      map((entities) => !entities.length),
      share()
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements),
      share()
    );
  }
}
