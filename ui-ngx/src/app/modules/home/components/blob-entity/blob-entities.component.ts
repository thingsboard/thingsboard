// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnInit,
  Optional,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { WidgetContext } from '@home/models/widget-component.models';
import { BlobEntitiesWidgetSettings } from '@home/components/blob-entity/blob-entities.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TimePageLink } from '@shared/models/page/page-link';
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { blobEntityTypeTranslationMap, BlobEntityWithCustomerInfo } from '@shared/models/blob-entity.models';
import { BehaviorSubject, forkJoin, merge, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { BlobEntityService } from '@core/http/blob-entity.service';
import { catchError, debounceTime, distinctUntilChanged, map, takeUntil, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { Direction, SortOrder, sortOrderFromString } from '@shared/models/page/sort-order';
import { DAY, historyInterval, HistoryWindowType, Timewindow } from '@shared/models/time/time.models';
import { isDefined, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { DialogService } from '@core/services/dialog.service';
import { UtilsService } from '@core/services/utils.service';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { FormBuilder } from '@angular/forms';
import { isValidPageStepCount, isValidPageStepIncrement } from '@home/components/widget/lib/table-widget.models';
import { WidgetComponent } from "@home/components/widget/widget.component";

@Component({
    selector: 'tb-blob-entities',
    templateUrl: './blob-entities.component.html',
    styleUrls: ['./blob-entities.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class BlobEntitiesComponent extends PageComponent implements OnInit, AfterViewInit {

  @ViewChild('blobEntitiesWidgetContainer', {static: true}) blobEntitiesWidgetContainerRef: ElementRef;
  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @Input()
  widgetMode: boolean;

  @Input()
  ctx: WidgetContext;

  settings: BlobEntitiesWidgetSettings;

  deleteEnabled = this.userPermissionsService.hasGenericPermission(Resource.BLOB_ENTITY, Operation.DELETE);

  authUser = getCurrentAuthUser(this.store);

  showData = (this.authUser.authority === Authority.TENANT_ADMIN ||
    this.authUser.authority === Authority.CUSTOMER_USER) &&
    this.userPermissionsService.hasGenericPermission(Resource.BLOB_ENTITY, Operation.READ);

  displayCreatedTime = true;
  displayType = true;
  displayCustomer = true;

  displayPagination = true;
  pageSizeOptions = [];
  defaultPageSize;
  defaultSortOrder = 'createdTime';
  defaultType: string;
  hidePageSize = false;

  displayedColumns: string[];
  timewindow: Timewindow;
  pageLink: TimePageLink;

  noDataDisplayMessageText: string;

  textSearchMode = false;

  dataSource: BlobEntitiesDatasource;

  textSearch = this.fb.control('', {nonNullable: true});

  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              public translate: TranslateService,
              private blobEntityService: BlobEntityService,
              private userPermissionsService: UserPermissionsService,
              private dialogService: DialogService,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder,
              private zone: NgZone,
              @Optional() public widgetComponent: WidgetComponent) {
    super(store);
  }

  ngOnInit(): void {
    if (this.widgetMode) {
      this.ctx.$scope.blobEntitiesWidget = this;
    }
    if (this.showData && this.widgetMode) {
      this.settings = this.ctx.settings;
      this.initializeWidgetConfig();
      this.ctx.updateWidgetParams();
    } else {
      this.displayedColumns = ['createdTime', 'name', 'type', 'customerTitle', 'actions'];
      if (this.deleteEnabled) {
        this.displayedColumns.unshift('select');
      }
      const sortOrder: SortOrder = { property: this.defaultSortOrder, direction: Direction.ASC };
      this.defaultPageSize = 10;
      this.pageSizeOptions = [this.defaultPageSize, this.defaultPageSize * 2, this.defaultPageSize * 3];
      this.timewindow = historyInterval(DAY);
      const currentTime = Date.now();
      this.pageLink = new TimePageLink(this.defaultPageSize, 0, null, sortOrder,
        currentTime - this.timewindow.history.timewindowMs, currentTime);
      this.dataSource = new BlobEntitiesDatasource(this.blobEntityService, this.translate);
    }
    if (this.displayPagination) {
      this.widgetResize$ = new ResizeObserver(() => {
        this.zone.run(() => {
          const showHidePageSize = this.blobEntitiesWidgetContainerRef.nativeElement.offsetWidth < hidePageSizePixelValue;
          if (showHidePageSize !== this.hidePageSize) {
            this.hidePageSize = showHidePageSize;
            this.cd.markForCheck();
          }
        });
      });
      this.widgetResize$.observe(this.blobEntitiesWidgetContainerRef.nativeElement);
    }
  }

  ngOnDestroy(): void {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeWidgetConfig() {
    this.ctx.widgetConfig.showTitle = false;
    this.ctx.widgetTitle = this.settings.title;
    const displayCreatedTime = isDefined(this.settings.displayCreatedTime) ? this.settings.displayCreatedTime : true;
    const displayType = isDefined(this.settings.displayType) ? this.settings.displayType : true;
    const displayCustomer = isDefined(this.settings.displayCustomer) ? this.settings.displayCustomer : true;

    this.displayedColumns = [];
    if (this.deleteEnabled) {
      this.displayedColumns.push('select');
    }
    if (displayCreatedTime) {
      this.displayedColumns.push('createdTime');
    }
    this.displayedColumns.push('name');
    if (displayType) {
      this.displayedColumns.push('type');
    }
    if (displayCustomer) {
      this.displayedColumns.push('customerTitle');
    }
    this.displayedColumns.push('actions');
    this.displayPagination = isDefined(this.settings.displayPagination) ? this.settings.displayPagination : true;
    const pageSize = this.settings.defaultPageSize;
    let pageStepIncrement = isValidPageStepIncrement(this.settings.pageStepIncrement) ? this.settings.pageStepIncrement : null;
    let pageStepCount = isValidPageStepCount(this.settings.pageStepCount) ? this.settings.pageStepCount : null;

    if (Number.isInteger(pageSize) && pageSize > 0) {
      this.defaultPageSize = pageSize;
    }

    if (!this.defaultPageSize) {
      this.defaultPageSize = pageStepIncrement ?? 10;
    }

    if (!isDefinedAndNotNull(pageStepIncrement) || !isDefinedAndNotNull(pageStepCount)) {
      pageStepIncrement = this.defaultPageSize;
      pageStepCount = 3;
    }

    for (let i = 1; i <= pageStepCount; i++) {
      this.pageSizeOptions.push(pageStepIncrement * i);
    }

    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this.defaultSortOrder = this.settings.defaultSortOrder;
    }
    const sortOrder: SortOrder = sortOrderFromString(this.defaultSortOrder);
    if (sortOrder.property === 'customer') {
      sortOrder.property = 'customerTitle';
    }
    this.timewindow = historyInterval(DAY);
    const currentTime = Date.now();
    this.pageLink = new TimePageLink(this.defaultPageSize, 0, null, sortOrder,
      currentTime - this.timewindow.history.timewindowMs, currentTime);

    const noDataDisplayMessage = this.settings.noDataDisplayMessage;
    if (isNotEmptyStr(noDataDisplayMessage)) {
      this.noDataDisplayMessageText = this.utils.customTranslation(noDataDisplayMessage, noDataDisplayMessage);
    } else {
      this.noDataDisplayMessageText = this.translate.instant('blob-entity.no-blob-entities-prompt');
    }

    if (this.settings.forceDefaultType && this.settings.forceDefaultType.length) {
      this.defaultType = this.settings.forceDefaultType;
    }
    this.ctx.widgetActions = [
      {
        name: 'action.search',
        show: true,
        icon: 'search',
        onAction: () => {
          this.enterFilterMode();
        }
      },
      {
        name: 'action.refresh',
        show: true,
        icon: 'refresh',
        onAction: () => {
          this.reloadBlobEntities();
        }
      }
    ];
    this.dataSource = new BlobEntitiesDatasource(this.blobEntityService, this.translate);
    this.dataSource.selection.changed.subscribe(() => {
      const hideTitlePanel = !this.dataSource.selection.isEmpty() || this.textSearchMode;
      if (this.ctx.hideTitlePanel !== hideTitlePanel) {
        this.ctx.hideTitlePanel = hideTitlePanel;
        this.ctx.detectChanges(true);
      } else {
        this.ctx.detectChanges();
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.showData) {
      this.textSearch.valueChanges.pipe(
        debounceTime(150),
        distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
        takeUntil(this.destroy$)
      ).subscribe((value) => {
        if (this.displayPagination) {
          this.paginator.pageIndex = 0;
        }
        this.pageLink.textSearch = value.trim();
        this.updateData();
      });

      if (this.displayPagination) {
        this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => this.paginator.pageIndex = 0);
      }

      ((this.displayPagination ? merge(this.sort.sortChange, this.paginator.page) : this.sort.sortChange) as Observable<any>).pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => this.updateData());

      this.updateData();
    }
  }

  onEditModeChanged() {
    if (this.textSearchMode) {
      this.ctx.hideTitlePanel = !this.ctx.isEdit;
      this.ctx.detectChanges(true);
    }
  }

  resize() {}

  updateData() {
    if (this.displayPagination) {
      this.pageLink.page = this.paginator.pageIndex;
      this.pageLink.pageSize = this.paginator.pageSize;
    } else {
      this.pageLink.page = 0;
    }
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    if (this.timewindow.history.historyType === HistoryWindowType.LAST_INTERVAL) {
      const currentTime = Date.now();
      this.pageLink.startTime = currentTime - this.timewindow.history.timewindowMs;
      this.pageLink.endTime = currentTime;
    } else {
      this.pageLink.startTime = this.timewindow.history.fixedTimewindow.startTimeMs;
      this.pageLink.endTime = this.timewindow.history.fixedTimewindow.endTimeMs;
    }
    if (this.showData) {
      this.dataSource.loadEntities(this.pageLink, this.defaultType);
    }
    if (this.widgetMode) {
      this.ctx.detectChanges();
    }
  }

  onTimewindowChange() {
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
    this.updateData();
  }

  enterFilterMode() {
    this.textSearchMode = true;
    if (this.widgetMode) {
      this.ctx.hideTitlePanel = true;
      this.ctx.detectChanges(true);
    }
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
    if (this.widgetMode) {
      this.ctx.hideTitlePanel = false;
      this.ctx.detectChanges(true);
    }
  }

  reloadBlobEntities() {
    this.updateData();
  }

  deleteBlobEntity($event: Event, blobEntity: BlobEntityWithCustomerInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('blob-entity.delete-blob-entity-title', {blobEntityName: blobEntity.name});
    const content = this.translate.instant('blob-entity.delete-blob-entity-text');
    this.dialogService.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe((result) => {
      if (result) {
        this.blobEntityService.deleteBlobEntity(blobEntity.id.id).subscribe(
          () => {
            this.reloadBlobEntities();
          }
        );
      }
    });
  }

  deleteBlobEntities($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const selectedBlobEntities = this.dataSource.selection.selected;
    if (selectedBlobEntities && selectedBlobEntities.length) {
      const title = this.translate.instant('blob-entity.delete-blob-entities-title', {count: selectedBlobEntities.length});
      const content = this.translate.instant('blob-entity.delete-blob-entities-text');
      this.dialogService.confirm(title, content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')).subscribe((result) => {
        if (result) {
          const tasks = selectedBlobEntities.map((blobEntity) =>
            this.blobEntityService.deleteBlobEntity(blobEntity.id.id));
          forkJoin(tasks).subscribe(
            () => {
              this.reloadBlobEntities();
            }
          );
        }
      });
    }
  }

  downloadBlobEntity($event, blobEntity: BlobEntityWithCustomerInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.blobEntityService.downloadBlobEntity(blobEntity.id.id).subscribe();
  }

}

class BlobEntitiesDatasource implements DataSource<BlobEntityWithCustomerInfo> {

  private entitiesSubject = new BehaviorSubject<BlobEntityWithCustomerInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<BlobEntityWithCustomerInfo>>(emptyPageData<BlobEntityWithCustomerInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<BlobEntityWithCustomerInfo>(true, []);

  public dataLoading = true;

  constructor(private blobEntityService: BlobEntityService,
              private translate: TranslateService) {
  }

  connect(collectionViewer: CollectionViewer):
    Observable<BlobEntityWithCustomerInfo[] | ReadonlyArray<BlobEntityWithCustomerInfo>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  reset() {
    const pageData = emptyPageData<BlobEntityWithCustomerInfo>();
    this.entitiesSubject.next(pageData.data);
    this.pageDataSubject.next(pageData);
  }

  loadEntities(pageLink: TimePageLink, type: string): Observable<PageData<BlobEntityWithCustomerInfo>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<BlobEntityWithCustomerInfo>>();
    this.fetchEntities(type, pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<BlobEntityWithCustomerInfo>())),
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

  fetchEntities(type: string,
                pageLink: TimePageLink): Observable<PageData<BlobEntityWithCustomerInfo>> {
    return this.blobEntityService.getBlobEntities(pageLink, type).pipe(
      map((data) => {
        data.data.forEach((blobEntity) => {
          blobEntity.typeName = blobEntity.type;
          if (blobEntityTypeTranslationMap.has(blobEntity.type)) {
            blobEntity.typeName = this.translate.instant(blobEntityTypeTranslationMap.get(blobEntity.type));
          }
        });
        return data;
      })
    );
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
    const entities = this.entitiesSubject.getValue();
    const numSelected = this.selection.selected.length;
    if (numSelected === entities.length) {
      this.selection.clear();
    } else {
      entities.forEach(row => {
        this.selection.select(row);
      });
    }
  }

}
