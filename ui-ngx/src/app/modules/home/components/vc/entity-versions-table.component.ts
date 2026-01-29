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
  ElementRef,
  EventEmitter,
  Input, NgZone,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityId, entityIdEquals } from '@shared/models/id/entity-id';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, merge, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, debounceTime, distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { EntityVersion, VersionCreationResult, VersionLoadResult } from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { BranchAutocompleteComponent } from '@shared/components/vc/branch-autocomplete.component';
import { isNotEmptyStr } from '@core/utils';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityVersionCreateComponent } from '@home/components/vc/entity-version-create.component';
import { MatButton } from '@angular/material/button';
import { EntityVersionRestoreComponent } from '@home/components/vc/entity-version-restore.component';
import { EntityVersionDiffComponent } from '@home/components/vc/entity-version-diff.component';
import { ComplexVersionCreateComponent } from '@home/components/vc/complex-version-create.component';
import { ComplexVersionLoadComponent } from '@home/components/vc/complex-version-load.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { AdminService } from '@core/http/admin.service';
import { FormBuilder } from '@angular/forms';

@Component({
    selector: 'tb-entity-versions-table',
    templateUrl: './entity-versions-table.component.html',
    styleUrls: ['./entity-versions-table.component.scss'],
    standalone: false
})
export class EntityVersionsTableComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('branchAutocompleteComponent') branchAutocompleteComponent: BranchAutocompleteComponent;

  @Input()
  singleEntityMode = false;

  @Input()
  popoverComponent: TbPopoverComponent;

  @Input()
  onBeforeCreateVersion: () => Observable<any>;

  displayedColumns = ['timestamp', 'id', 'name', 'author', 'actions'];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: EntityVersionsDatasource;
  hidePageSize = false;

  branch: string = null;

  activeValue = false;
  dirtyValue = false;
  externalEntityIdValue: EntityId;

  viewsInited = false;

  isReadOnly: Observable<boolean>;

  textSearch = this.fb.control('', {nonNullable: true});

  private componentResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

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
    if (!entityIdEquals(this.externalEntityIdValue, externalEntityId)) {
      this.externalEntityIdValue = externalEntityId;
      this.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  entityId: EntityId;

  @Input()
  entityName: string;

  @Output()
  versionRestored = new EventEmitter<void>();

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private adminService: AdminService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef,
              private elementRef: ElementRef,
              private fb: FormBuilder,
              private zone: NgZone) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'timestamp', direction: Direction.DESC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new EntityVersionsDatasource(this.entitiesVersionControlService);
  }

  ngOnInit() {
    this.componentResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
    });
    this.componentResize$.observe(this.elementRef.nativeElement);
    this.isReadOnly = this.adminService.getRepositorySettingsInfo().pipe(map(settings => settings.readOnly));
  }

  ngOnDestroy() {
    if (this.componentResize$) {
      this.componentResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
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
    this.textSearch.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.paginator.pageIndex = 0;
      this.pageLink.textSearch = value.trim();
      this.updateData();
    });

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);
    merge(this.sort.sortChange, this.paginator.page).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateData());
    this.viewsInited = true;
    if (!this.singleEntityMode || (this.activeValue && this.externalEntityIdValue)) {
      this.initFromDefaultBranch();
    }
  }

  toggleCreateVersion($event: Event, createVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = createVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const createVersionPopover =  this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EntityVersionCreateComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          branch: this.branch,
          entityId: this.entityId,
          entityName: this.entityName,
          onBeforeCreateVersion: this.onBeforeCreateVersion,
          onClose: (result: VersionCreationResult | null, branch: string | null) => {
            createVersionPopover.hide();
            if (result) {
              if (this.branch !== branch) {
                this.branchChanged(branch);
              } else {
                this.updateData();
              }
            }
          }
        },
        showCloseButton: false,
        overlayStyle: {maxHeight: '100vh', height: '100%', padding: '10px'},
        popoverStyle: {width: '400px', minWidth: '100%', maxWidth: '100%'},
        isModal: true
      });
      createVersionPopover.tbComponentRef.instance.popoverComponent = createVersionPopover;
    }
  }

  toggleComplexCreateVersion($event: Event, complexCreateVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = complexCreateVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const complexCreateVersionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ComplexVersionCreateComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          branch: this.branch,
          onClose: (result: VersionCreationResult | null, branch: string | null) => {
            complexCreateVersionPopover.hide();
            if (result) {
              if (this.branch !== branch) {
                this.branchChanged(branch);
              } else {
                this.updateData();
              }
            }
          }
        },
        showCloseButton: false,
        overlayStyle: {maxHeight: '90vh', height: '100%', padding: '10px'},
        isModal: true
      });
      complexCreateVersionPopover.tbComponentRef.instance.popoverComponent = complexCreateVersionPopover;
    }
  }

  toggleShowVersionDiff($event: Event, diffVersionButton: MatButton, entityVersion: EntityVersion) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = diffVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const diffVersionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EntityVersionDiffComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          versionName: entityVersion.name,
          versionId: entityVersion.id,
          entityId: this.entityId,
          externalEntityId: this.externalEntityIdValue
        },
        showCloseButton: false,
        isModal: true
      });
      diffVersionPopover.tbComponentRef.instance.popoverComponent = diffVersionPopover;
      diffVersionPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
        this.versionRestored.emit();
      });
    }
  }

  toggleRestoreEntityVersion($event: Event, restoreVersionButton: MatButton, entityVersion: EntityVersion) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = restoreVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const restoreVersionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EntityVersionRestoreComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          versionName: entityVersion.name,
          versionId: entityVersion.id,
          externalEntityId: this.externalEntityIdValue,
          onClose: (result: VersionLoadResult | null) => {
            restoreVersionPopover.hide();
            if (result && !result.error && result.result.length) {
              this.versionRestored.emit();
            }
          }
        },
        showCloseButton: false,
        overlayStyle: {maxHeight: '100vh', height: '100%', padding: '10px'},
        popoverStyle: {width: '400px', minWidth: '100%', maxWidth: '100%'},
        isModal: true
      });
      restoreVersionPopover.tbComponentRef.instance.popoverComponent = restoreVersionPopover;
    }
  }

  toggleRestoreEntitiesVersion($event: Event, restoreEntitiesVersionButton: MatButton, entityVersion: EntityVersion) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = restoreEntitiesVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const restoreEntitiesVersionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ComplexVersionLoadComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          versionName: entityVersion.name,
          versionId: entityVersion.id,
          onClose: (result: VersionLoadResult | null) => {
            restoreEntitiesVersionPopover.hide();
          }
        },
        showCloseButton: false,
        overlayStyle: {maxHeight: '80vh', height: '100%', padding: '10px'},
        isModal: true
      });
      restoreEntitiesVersionPopover.tbComponentRef.instance.popoverComponent = restoreEntitiesVersionPopover;
    }
  }

  versionIdContent(entityVersion: EntityVersion): string {
    let versionId = entityVersion.id;
    if (versionId.length > 7) {
      versionId = versionId.slice(0, 7);
    }
    return versionId;
  }

  enterFilterMode() {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
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

  updateData() {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadEntityVersions(this.singleEntityMode, this.branch, this.externalEntityIdValue, this.pageLink);
  }

  private resetSortAndFilter(update: boolean) {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.textSearch.reset('', {emitEvent: false});
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
