///
/// Copyright © 2016-2022 The Thingsboard Authors
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
  ElementRef,
  Input,
  OnDestroy,
  OnInit, Renderer2,
  ViewChild, ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityId } from '@shared/models/id/entity-id';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, merge, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, tap } from 'rxjs/operators';
import { EntityVersion, VersionCreationResult } from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { ResizeObserver } from '@juggle/resize-observer';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { BranchAutocompleteComponent } from '@shared/components/vc/branch-autocomplete.component';
import { isNotEmptyStr } from '@core/utils';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityVersionExportComponent } from '@home/components/vc/entity-version-export.component';
import { MatButton } from '@angular/material/button';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
  selector: 'tb-entity-versions-table',
  templateUrl: './entity-versions-table.component.html',
  styleUrls: ['./entity-versions-table.component.scss']
})
export class EntityVersionsTableComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('branchAutocompleteComponent') branchAutocompleteComponent: BranchAutocompleteComponent;

  @Input()
  singleEntityMode = false;

  displayedColumns = ['timestamp', 'id', 'name'];
  pageLink: PageLink;
  dataSource: EntityVersionsDatasource;
  hidePageSize = false;

  branch: string = null;

  activeValue = false;
  dirtyValue = false;
  externalEntityIdValue: EntityId;

  viewsInited = false;

  vcExportPopover: TbPopoverComponent;

  private componentResize$: ResizeObserver;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        if (this.viewsInited) {
          this.initFromDefaultBranch();
        }
      }
    }
  }

  @Input()
  set externalEntityId(externalEntityId: EntityId) {
    if (this.externalEntityIdValue !== externalEntityId) {
      this.externalEntityIdValue = externalEntityId;
      this.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  entityId: EntityId;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef,
              private elementRef: ElementRef) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'timestamp', direction: Direction.DESC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new EntityVersionsDatasource(this.entitiesVersionControlService);
  }

  ngOnInit() {
    this.componentResize$ = new ResizeObserver(() => {
      const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
      if (showHidePageSize !== this.hidePageSize) {
        this.hidePageSize = showHidePageSize;
        this.cd.markForCheck();
      }
    });
    this.componentResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy() {
    if (this.componentResize$) {
      this.componentResize$.disconnect();
    }
  }

  branchChanged(newBranch: string) {
    if (isNotEmptyStr(newBranch) && this.branch !== newBranch) {
      this.branch = newBranch;
      this.paginator.pageIndex = 0;
      if (this.activeValue) {
        this.updateData();
      }
    }
  }

  ngAfterViewInit() {
    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);
    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();
    this.viewsInited = true;
    if (!this.singleEntityMode) {
      this.initFromDefaultBranch();
    }
  }

  toggleVcExport($event: Event, exportButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = exportButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.vcExportPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, EntityVersionExportComponent, 'bottom', true, null,
        {
          branch: this.branch,
          entityId: this.entityId,
          onClose: (result: VersionCreationResult | null, branch: string | null) => {
            this.vcExportPopover.hide();
            if (result) {
              if (this.branch !== branch) {
                this.branchChanged(branch);
              } else {
                this.updateData();
              }
            }
          }
        }, {}, {}, {}, false);
      this.vcExportPopover.tbVisibleChange.subscribe((visible: boolean) => {
        if (!visible) {
          this.vcExportPopover = null;
        }
      });
    }
  }

  versionIdContent(entityVersion: EntityVersion): string {
    let versionId = entityVersion.id;
    if (versionId.length > 7) {
      versionId = versionId.slice(0, 7);
    }
    return versionId;
  }

  private initFromDefaultBranch() {
    if (this.branchAutocompleteComponent.isDefaultBranchSelected()) {
      this.paginator.pageIndex = 0;
      if (this.activeValue) {
        this.updateData();
      }
    } else {
      this.branchAutocompleteComponent.selectDefaultBranchIfNeeded(true);
    }
  }

  private updateData() {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadEntityVersions(this.singleEntityMode, this.branch, this.externalEntityIdValue, this.pageLink);
  }

  private resetSortAndFilter(update: boolean) {
    this.pageLink.textSearch = null;
    if (this.viewsInited) {
      this.paginator.pageIndex = 0;
      const sortable = this.sort.sortables.get('timestamp');
      this.sort.active = sortable.id;
      this.sort.direction = 'desc';
      if (update) {
        this.initFromDefaultBranch();
      }
    }
  }
}

class EntityVersionsDatasource implements DataSource<EntityVersion> {

  private entityVersionsSubject = new BehaviorSubject<EntityVersion[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<EntityVersion>>(emptyPageData<EntityVersion>());

  public pageData$ = this.pageDataSubject.asObservable();

  public dataLoading = true;

  constructor(private entitiesVersionControlService: EntitiesVersionControlService) {}

  connect(collectionViewer: CollectionViewer): Observable<EntityVersion[] | ReadonlyArray<EntityVersion>> {
    return this.entityVersionsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entityVersionsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadEntityVersions(singleEntityMode: boolean,
                     branch: string, externalEntityId: EntityId,
                     pageLink: PageLink): Observable<PageData<EntityVersion>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<EntityVersion>>();
    this.fetchEntityVersions(singleEntityMode, branch, externalEntityId, pageLink).pipe(
      catchError(() => of(emptyPageData<EntityVersion>())),
    ).subscribe(
      (pageData) => {
        this.entityVersionsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoading = false;
      }
    );
    return result;
  }

  fetchEntityVersions(singleEntityMode: boolean,
                      branch: string, externalEntityId: EntityId,
                      pageLink: PageLink): Observable<PageData<EntityVersion>> {
    if (!branch) {
      return of(emptyPageData<EntityVersion>());
    } else {
      if (singleEntityMode) {
        if (externalEntityId) {
          return this.entitiesVersionControlService.listEntityVersions(pageLink, branch, externalEntityId, {ignoreErrors: true});
        } else {
          return of(emptyPageData<EntityVersion>());
        }
      } else {
        return this.entitiesVersionControlService.listVersions(pageLink, branch, {ignoreErrors: true});
      }
    }
  }

  isEmpty(): Observable<boolean> {
    return this.entityVersionsSubject.pipe(
      map((entityVersions) => !entityVersions.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }
}
