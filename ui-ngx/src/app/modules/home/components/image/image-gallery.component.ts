///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { ImageResourceInfo, imageResourceType } from '@shared/models/resource.models';
import { BehaviorSubject, forkJoin, merge, Observable, of, ReplaySubject, Subject, Subscription } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { ImageService } from '@core/http/image.service';
import { TranslateService } from '@ngx-translate/core';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { catchError, debounceTime, distinctUntilChanged, map, skip, takeUntil, tap } from 'rxjs/operators';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, SortDirection } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { DialogService } from '@core/services/dialog.service';
import { FormBuilder } from '@angular/forms';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { ResizeObserver } from '@juggle/resize-observer';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ActivatedRoute, QueryParamsHandling, Router } from '@angular/router';
import { isEqual, isNotEmptyStr } from '@core/utils';
import { BaseData, HasId } from '@shared/models/base-data';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { GridEntitiesFetchFunction, ScrollGridColumns } from '@home/models/datasource/scroll-grid-datasource';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { ScrollGridComponent } from '@home/components/grid/scroll-grid.component';
import {
  AddWidgetDialogComponent,
  AddWidgetDialogData
} from '@home/components/dashboard-page/add-widget-dialog.component';
import { Widget } from '@shared/models/widget.models';
import { MatDialog } from '@angular/material/dialog';
import { UploadImageDialogComponent } from '@home/components/image/upload-image-dialog.component';

@Component({
  selector: 'tb-image-gallery',
  templateUrl: './image-gallery.component.html',
  styleUrls: ['./image-gallery.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ImageGalleryComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {

  @Input()
  @coerceBoolean()
  pageMode = true;

  @Input()
  mode: 'list' | 'grid' = 'list';

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @ViewChild(ScrollGridComponent) gridComponent: ScrollGridComponent<ImageResourceInfo, string>;

  defaultPageSize = 10;
  defaultSortOrder: SortOrder = { property: 'createdTime', direction: Direction.DESC };
  hidePageSize = false;

  displayedColumns: string[];
  pageSizeOptions: number[];
  pageLink: PageLink;

  textSearchMode = false;

  dataSource: ImagesDatasource;

  textSearch = this.fb.control('', {nonNullable: true});

  gridColumns: ScrollGridColumns = {
    columns: 2,
    breakpoints: {
      'screen and (min-width: 2320px)': 10,
      'screen and (min-width: 2000px)': 8,
      'gt-lg': 7,
      'screen and (min-width: 1600px)': 6,
      'gt-md': 5,
      'screen and (min-width: 1120px)': 4,
      'gt-xs': 3
    }
  };

  gridImagesFetchFunction: GridEntitiesFetchFunction<ImageResourceInfo, string>;
  gridImagesFilter = '';

  authUser = getCurrentAuthUser(this.store);

  private updateDataSubscription: Subscription;

  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();
  private destroyListMode$: Subject<void>;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private dialog: MatDialog,
              public translate: TranslateService,
              private imageService: ImageService,
              private dialogService: DialogService,
              private elementRef: ElementRef,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder) {
    super(store);

    this.gridImagesFetchFunction = (pageSize, page, filter) => {
      const pageLink = new PageLink(pageSize, page, filter, {
        property: 'createdTime',
        direction: Direction.DESC
      });
      return this.imageService.getImages(pageLink);
    };
  }

  ngOnInit(): void {
    this.displayedColumns = ['select', 'preview', 'title', 'createdTime', 'resolution', 'size', 'system', 'actions'];
    let sortOrder: SortOrder = this.defaultSortOrder;
    this.pageSizeOptions = [this.defaultPageSize, this.defaultPageSize * 2, this.defaultPageSize * 3];
    const routerQueryParams: PageQueryParam = this.route.snapshot.queryParams;
    if (this.pageMode) {
      if (routerQueryParams.hasOwnProperty('direction')
        || routerQueryParams.hasOwnProperty('property')) {
        sortOrder = {
          property: routerQueryParams?.property || this.defaultSortOrder.property,
          direction: routerQueryParams?.direction || this.defaultSortOrder.direction
        };
      }
    }
    this.pageLink = new PageLink(this.defaultPageSize, 0, null, sortOrder);
    if (this.pageMode) {
      if (routerQueryParams.hasOwnProperty('page')) {
        this.pageLink.page = Number(routerQueryParams.page);
      }
      if (routerQueryParams.hasOwnProperty('pageSize')) {
        this.pageLink.pageSize = Number(routerQueryParams.pageSize);
      }
      const textSearchParam = routerQueryParams.textSearch;
      if (isNotEmptyStr(textSearchParam)) {
        const decodedTextSearch = decodeURI(textSearchParam);
        this.textSearchMode = true;
        this.pageLink.textSearch = decodedTextSearch.trim();
        this.textSearch.setValue(decodedTextSearch, {emitEvent: false});
      }
    }
    if (this.mode === 'list') {
      this.dataSource = new ImagesDatasource(this.imageService);
    }
  }

  ngOnDestroy(): void {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    if (this.destroyListMode$) {
      this.destroyListMode$.next();
      this.destroyListMode$.complete();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) =>
        ((this.mode === 'list' ? this.pageLink.textSearch : this.gridImagesFilter) ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (this.mode === 'list') {
        if (this.pageMode) {
          const queryParams: PageQueryParam = {
            textSearch: isNotEmptyStr(value) ? encodeURI(value) : null,
            page: null
          };
          this.updatedRouterParamsAndData(queryParams);
        } else {
          this.pageLink.textSearch = isNotEmptyStr(value) ? value.trim() : null;
          this.paginator.pageIndex = 0;
          this.updateData();
        }
      } else {
        this.gridImagesFilter = isNotEmptyStr(value) ? value.trim() : null;
        this.cd.markForCheck();
      }
    });
    this.updateMode();
  }

  public setMode(targetMode: 'list' | 'grid') {
    if (this.mode !== targetMode) {
      if (this.widgetResize$) {
        this.widgetResize$.disconnect();
        this.widgetResize$ = null;
      }
      if (this.destroyListMode$) {
        this.destroyListMode$.next();
        this.destroyListMode$.complete();
        this.destroyListMode$ = null;
      }
      this.mode = targetMode;
      if (this.mode === 'list') {
        this.dataSource = new ImagesDatasource(this.imageService);
      }
      setTimeout(() => {
        this.updateMode();
      });
    }
  }

  private updateMode() {
    if (this.mode === 'list') {
      this.initListMode();
    } else {
      this.initGridMode();
    }
  }

  private initListMode() {
    this.destroyListMode$ = new Subject<void>();
    this.widgetResize$ = new ResizeObserver(() => {
      const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
      if (showHidePageSize !== this.hidePageSize) {
        this.hidePageSize = showHidePageSize;
        this.cd.markForCheck();
      }
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
    if (this.pageMode) {
      this.route.queryParams.pipe(
        skip(1),
        takeUntil(this.destroyListMode$)
      ).subscribe((params: PageQueryParam) => {
        this.paginator.pageIndex = Number(params.page) || 0;
        this.paginator.pageSize = Number(params.pageSize) || this.defaultPageSize;
        this.sort.active = params.property || this.defaultSortOrder.property;
        this.sort.direction = (params.direction || this.defaultSortOrder.direction).toLowerCase() as SortDirection;
        const textSearchParam = params.textSearch;
        if (isNotEmptyStr(textSearchParam)) {
          const decodedTextSearch = decodeURI(textSearchParam);
          this.textSearchMode = true;
          this.pageLink.textSearch = decodedTextSearch.trim();
          this.textSearch.setValue(decodedTextSearch, {emitEvent: false});
        } else {
          this.pageLink.textSearch = null;
          this.textSearch.reset('', {emitEvent: false});
        }
        this.updateData();
      });
    }
    this.updatePaginationSubscriptions();
    this.updateData();
  }

  private initGridMode() {

  }

  private updatePaginationSubscriptions() {
    if (this.updateDataSubscription) {
      this.updateDataSubscription.unsubscribe();
      this.updateDataSubscription = null;
    }
    const sortSubscription$: Observable<object> = this.sort.sortChange.asObservable().pipe(
      map((data) => {
        const direction = data.direction.toUpperCase();
        const queryParams: PageQueryParam = {
          direction: (this.defaultSortOrder.direction === direction ? null : direction) as Direction,
          property: this.defaultSortOrder.property === data.active ? null : data.active
        };
        queryParams.page = null;
        this.paginator.pageIndex = 0;
        return queryParams;
      })
    );
    const paginatorSubscription$ = this.paginator.page.asObservable().pipe(
        map((data) => ({
          page: data.pageIndex === 0 ? null : data.pageIndex,
          pageSize: data.pageSize === this.defaultPageSize ? null : data.pageSize
        }))
      );
    this.updateDataSubscription = (merge(sortSubscription$, paginatorSubscription$) as Observable<PageQueryParam>).pipe(
      takeUntil(this.destroyListMode$)
    ).subscribe(queryParams => this.updatedRouterParamsAndData(queryParams));
  }

  clearSelection() {
    this.dataSource.selection.clear();
    this.cd.detectChanges();
  }

  updateData() {
    if (this.mode === 'list') {
      this.pageLink.page = this.paginator.pageIndex;
      this.pageLink.pageSize = this.paginator.pageSize;
      if (this.sort.active) {
        this.pageLink.sortOrder = {
          property: this.sort.active,
          direction: Direction[this.sort.direction.toUpperCase()]
        };
      } else {
        this.pageLink.sortOrder = null;
      }
      this.dataSource.loadEntities(this.pageLink);
    } else {
      this.gridComponent.update();
    }
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

  trackByEntityId(index: number, entity: BaseData<HasId>) {
    return entity.id.id;
  }

  isSystem(image?: ImageResourceInfo): boolean {
    return image?.tenantId?.id === NULL_UUID;
  }

  deleteEnabled(image?: ImageResourceInfo): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN || !this.isSystem(image);
  }

  deleteImage($event: Event, image: ImageResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('image.delete-image-title', {imageTitle: image.title});
    const content = this.translate.instant('image.delete-image-text');
    this.dialogService.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe((result) => {
      if (result) {
        this.imageService.deleteImage(imageResourceType(image), image.resourceKey).subscribe(
          () => {
            this.updateData();
          }
        );
      }
    });
  }

  deleteImages($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const selectedImages = this.dataSource.selection.selected;
    if (selectedImages && selectedImages.length) {
      const title = this.translate.instant('image.delete-images-title', {count: selectedImages.length});
      const content = this.translate.instant('image.delete-images-text');
      this.dialogService.confirm(title, content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')).subscribe((result) => {
        if (result) {
          const tasks = selectedImages.map((image) =>
            this.imageService.deleteImage(imageResourceType(image), image.resourceKey));
          forkJoin(tasks).subscribe(
            () => {
              this.updateData();
            }
          );
        }
      });
    }
  }

  downloadImage($event, image: ImageResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageService.downloadImage(imageResourceType(image), image.resourceKey).subscribe();
  }

  exportImage($event, image: ImageResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
  }

  importImage(): void {
    // TODO:
  }

  uploadImage(): void {
    this.dialog.open<UploadImageDialogComponent, any,
      boolean>(UploadImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.updateData();
      }
    });
  }

  editImage($event, image: ImageResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
  }

  protected updatedRouterParamsAndData(queryParams: object, queryParamsHandling: QueryParamsHandling = 'merge') {
    if (this.pageMode) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams,
        queryParamsHandling
      });
      if (queryParamsHandling === '' && isEqual(this.route.snapshot.queryParams, queryParams)) {
        this.updateData();
      }
    } else {
      this.updateData();
    }
  }

}

class ImagesDatasource implements DataSource<ImageResourceInfo> {
  private entitiesSubject = new BehaviorSubject<ImageResourceInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<ImageResourceInfo>>(emptyPageData<ImageResourceInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<ImageResourceInfo>(true, []);

  public dataLoading = true;

  constructor(private imageService: ImageService) {
  }

  connect(collectionViewer: CollectionViewer):
    Observable<ImageResourceInfo[] | ReadonlyArray<ImageResourceInfo>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  reset() {
    const pageData = emptyPageData<ImageResourceInfo>();
    this.entitiesSubject.next(pageData.data);
    this.pageDataSubject.next(pageData);
  }

  loadEntities(pageLink: PageLink): Observable<PageData<ImageResourceInfo>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<ImageResourceInfo>>();
    this.fetchEntities(pageLink).pipe(
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

  fetchEntities(pageLink: PageLink): Observable<PageData<ImageResourceInfo>> {
    return this.imageService.getImages(pageLink);
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
