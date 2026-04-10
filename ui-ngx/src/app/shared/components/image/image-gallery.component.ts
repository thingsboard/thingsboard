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
  ImageResourceInfo,
  ResourceInfoWithReferences,
  imageResourceType,
  ResourceSubType,
  toResourceDeleteResult
} from '@shared/models/resource.models';
import { forkJoin, merge, Observable, of, Subject, Subscription } from 'rxjs';
import { ImageService } from '@core/http/image.service';
import { TranslateService } from '@ngx-translate/core';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { catchError, debounceTime, distinctUntilChanged, map, skip, takeUntil } from 'rxjs/operators';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input, NgZone,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, SortDirection } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogService } from '@core/services/dialog.service';
import { FormBuilder } from '@angular/forms';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ActivatedRoute, QueryParamsHandling, Router } from '@angular/router';
import { isEqual, isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { BaseData, HasId } from '@shared/models/base-data';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { GridEntitiesFetchFunction, ScrollGridColumns } from '@shared/components/grid/scroll-grid-datasource';
import { ItemSizeStrategy, ScrollGridComponent } from '@shared/components/grid/scroll-grid.component';
import { MatDialog } from '@angular/material/dialog';
import {
  UploadImageDialogComponent,
  UploadImageDialogData, UploadImageDialogResult
} from '@shared/components/image/upload-image-dialog.component';
import { ImageDialogComponent, ImageDialogData } from '@shared/components/image/image-dialog.component';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  ResourcesInUseDialogComponent,
  ResourcesInUseDialogData
} from '@shared/components/resource/resources-in-use-dialog.component';
import { ImagesDatasource } from '@shared/components/image/images-datasource';
import { EmbedImageDialogComponent, EmbedImageDialogData } from '@shared/components/image/embed-image-dialog.component';

interface GridImagesFilter {
  search: string;
  includeSystemImages: boolean;
}

const pageGridColumns: ScrollGridColumns = {
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

const dialogGridColumns: ScrollGridColumns = {
  columns: 2,
  breakpoints: {
    'gt-md': 4,
    'gt-xs': 3
  }
};

@Component({
    selector: 'tb-image-gallery',
    templateUrl: './image-gallery.component.html',
    styleUrls: ['./image-gallery.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ImageGalleryComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {

  @HostBinding('style.display')
  private display = 'block';

  @HostBinding('style.width')
  private width = '100%';

  @HostBinding('style.height')
  private height = '100%';

  @Input()
  @coerceBoolean()
  pageMode = true;

  @Input()
  @coerceBoolean()
  dialogMode = false;

  @Input()
  imageSubType = ResourceSubType.IMAGE;

  @Input()
  mode: 'list' | 'grid' = 'list';

  @Input()
  @coerceBoolean()
  selectionMode = false;

  @Output()
  imageSelected = new EventEmitter<ImageResourceInfo>();

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
  includeSystemImages = false;

  gridColumns: ScrollGridColumns;

  gridImagesFetchFunction: GridEntitiesFetchFunction<ImageResourceInfo, GridImagesFilter>;
  gridImagesFilter: GridImagesFilter = {
    search: '',
    includeSystemImages: false
  };

  gridImagesItemSizeStrategy: ItemSizeStrategy = {
    defaultItemSize: 200,
    itemSizeFunction: itemWidth => itemWidth + 72
  };

  authUser = getCurrentAuthUser(this.store);

  get isScada() {
    return this.imageSubType === ResourceSubType.SCADA_SYMBOL;
  }

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
              private importExportService: ImportExportService,
              private elementRef: ElementRef,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder,
              private zone: NgZone) {
    super(store);

    this.gridImagesFetchFunction = (pageSize, page, filter) => {
      const pageLink = new PageLink(pageSize, page, filter.search, {
        property: 'createdTime',
        direction: Direction.DESC
      });
      return this.imageService.getImages(pageLink, filter.includeSystemImages, this.imageSubType);
    };
  }

  ngOnInit(): void {
    this.gridColumns = this.dialogMode ? dialogGridColumns : pageGridColumns;
    this.displayedColumns = this.computeDisplayedColumns();
    let sortOrder: SortOrder = this.defaultSortOrder;
    this.pageSizeOptions = [this.defaultPageSize, this.defaultPageSize * 2, this.defaultPageSize * 3];
    const routerQueryParams: PageQueryParam = this.route.snapshot.queryParams;
    if (this.pageMode) {
      this.imageSubType = this.route.snapshot.data.imageSubType || ResourceSubType.IMAGE;
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
      this.dataSource = new ImagesDatasource(this.imageService, null,
          entity => this.deleteEnabled(entity));
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
        ((this.mode === 'list' ? this.pageLink.textSearch : this.gridImagesFilter.search) ?? '') === current.trim()),
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
        this.gridImagesFilter = {
          search: isNotEmptyStr(value) ? value.trim() : null,
          includeSystemImages: this.includeSystemImages
        };
        this.cd.markForCheck();
      }
    });
    this.updateMode();
  }

  public includeSystemImagesChanged(value: boolean) {
    this.includeSystemImages = value;
    this.displayedColumns = this.computeDisplayedColumns();
    this.gridImagesFilter = {
      search: this.gridImagesFilter.search,
      includeSystemImages: this.includeSystemImages
    };
    if (this.mode === 'list') {
      this.paginator.pageIndex = 0;
      this.updateData();
    } else {
      this.cd.markForCheck();
    }
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
        this.dataSource = new ImagesDatasource(this.imageService, null,
          entity => this.deleteEnabled(entity));
      }
      setTimeout(() => {
        this.updateMode();
      });
    }
  }

  public get isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private computeDisplayedColumns(): string[] {
    let columns: string[];
    if (this.selectionMode) {
      columns = ['preview', 'title'];
      if (!this.isSysAdmin && this.includeSystemImages) {
        columns.push('system');
      }
      columns.push('imageSelect');
    } else {
      columns = ['select', 'preview', 'title', 'createdTime', 'resolution', 'size'];
      if (!this.isSysAdmin && this.includeSystemImages) {
        columns.push('system');
      }
      columns.push('actions');
    }
    return columns;
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
      this.zone.run(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
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
      this.dataSource.loadEntities(this.pageLink, this.imageSubType, this.includeSystemImages);
    } else {
      this.gridComponent.update();
    }
  }

  private imageUpdated(image: ImageResourceInfo, index = -1) {
    if (this.mode === 'list') {
      this.updateData();
    } else {
      this.gridComponent.updateItem(index, image);
    }
  }

  private imageDeleted(index = -1) {
    if (this.mode === 'list') {
      this.updateData();
    } else {
      this.gridComponent.deleteItem(index);
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

  trackByEntity(index: number, entity: BaseData<HasId>) {
    return entity;
  }

  isSystem(image?: ImageResourceInfo): boolean {
    return !this.isSysAdmin && image?.tenantId?.id === NULL_UUID;
  }

  readonly(image?: ImageResourceInfo): boolean {
    return this.authUser.authority !== Authority.SYS_ADMIN && this.isSystem(image);
  }

  deleteEnabled(image?: ImageResourceInfo): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN || !this.isSystem(image);
  }

  deleteImage($event: Event, image: ImageResourceInfo, itemIndex = -1) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant(this.isScada ? 'scada.delete-symbol-title' : 'image.delete-image-title',
      {imageTitle: image.title});
    const content = this.translate.instant(this.isScada ? 'scada.delete-symbol-text' : 'image.delete-image-text');
    this.dialogService.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe((result) => {
      if (result) {
        this.imageService.deleteImage(imageResourceType(image), image.resourceKey, false, {ignoreErrors: true}).pipe(
          map(() => toResourceDeleteResult(image)),
          catchError((err) => of(toResourceDeleteResult(image, err)))
        ).subscribe(
          (deleteResult) => {
            if (deleteResult.success) {
              this.imageDeleted(itemIndex);
            } else if (deleteResult.resourceIsReferencedError) {
              const images = [{...image, ...{references: deleteResult.references}}];
              const data = {
                multiple: false,
                resources: images,
                configuration: {
                  title: 'image.image-is-in-use',
                  message: this.translate.instant('image.image-is-in-use-text', {title: images[0].title}),
                  deleteText: 'image.delete-image-in-use-text',
                  selectedText: 'image.selected-images',
                  columns: ['select', 'preview', 'title', 'references']
                }
              };
              this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                ImageResourceInfo[]>(ResourcesInUseDialogComponent, {
                disableClose: true,
                panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                data
              }).afterClosed().subscribe((images) => {
                if (images) {
                  this.imageService.deleteImage(imageResourceType(image), image.resourceKey, true).subscribe(
                    () => {
                      this.imageDeleted(itemIndex);
                    }
                  );
                }
              });
            } else {
              const errorMessageWithTimeout = parseHttpErrorMessage(deleteResult.error, this.translate);
              setTimeout(() => {
                this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
              }, errorMessageWithTimeout.timeout);
            }
        });
      }
    });
  }

  deleteImages($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const selectedImages = this.dataSource.selection.selected;
    if (selectedImages && selectedImages.length) {
      const title = this.translate.instant(this.isScada ? 'scada.delete-symbols-title' : 'image.delete-images-title',
        {count: selectedImages.length});
      const content = this.translate.instant(this.isScada ? 'scada.delete-symbols-text' : 'image.delete-images-text');
      this.dialogService.confirm(title, content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')).subscribe((result) => {
        if (result) {
          const tasks = selectedImages.map((image) =>
            this.imageService.deleteImage(imageResourceType(image), image.resourceKey, false, {ignoreErrors: true}).pipe(
              map(() => toResourceDeleteResult(image)),
              catchError((err) => of(toResourceDeleteResult(image, err)))
            )
          );
          forkJoin(tasks).subscribe(
            (deleteResults) => {
              const anySuccess = deleteResults.some(res => res.success);
              const referenceErrors = deleteResults.filter(res => res.resourceIsReferencedError);
              const otherError = deleteResults.find(res => !res.success);
              if (anySuccess) {
                this.updateData();
              }
              if (referenceErrors?.length) {
                const imagesWithReferences: ResourceInfoWithReferences[] =
                  referenceErrors.map(ref => ({...ref.resource, ...{references: ref.references}}));
                const data = {
                  multiple: true,
                  resources: imagesWithReferences,
                  configuration: {
                    title: 'image.images-are-in-use',
                    message: this.translate.instant('image.images-are-in-use-text'),
                    deleteText: 'image.delete-image-in-use-text',
                    selectedText: 'image.selected-images',
                    columns: ['select', 'preview', 'title', 'references'],
                    datasource: new ImagesDatasource(null, imagesWithReferences, entity => true)
                  }
                };
                this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                  ImageResourceInfo[]>(ResourcesInUseDialogComponent, {
                  disableClose: true,
                  panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                  data
                }).afterClosed().subscribe((forceDeleteImages) => {
                  if (forceDeleteImages && forceDeleteImages.length) {
                    const forceDeleteTasks = forceDeleteImages.map((image) =>
                      this.imageService.deleteImage(imageResourceType(image), image.resourceKey, true)
                    );
                    forkJoin(forceDeleteTasks).subscribe(
                      () => {
                        this.updateData();
                      }
                    );
                  }
                });
              } else if (otherError) {
                const errorMessageWithTimeout = parseHttpErrorMessage(otherError.error, this.translate);
                setTimeout(() => {
                  this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
                }, errorMessageWithTimeout.timeout);
              }
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
    this.importExportService.exportImage(imageResourceType(image), image.resourceKey);
  }

  importImage(): void {
    this.importExportService.importImage().subscribe((image) => {
      if (image) {
        if (this.selectionMode) {
          this.imageSelected.next(image);
        } else {
          this.updateData();
        }
      }
    });
  }

  selectImage($event, image: ImageResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageSelected.next(image);
  }

  rowClick($event, image: ImageResourceInfo) {
    if (this.isScada) {
      this.editImage($event, image);
    } else {
      if (this.selectionMode) {
        this.selectImage($event, image);
      } else {
        if (this.deleteEnabled(image)) {
          this.dataSource.selection.toggle(image);
        }
      }
    }
  }

  uploadImage(): void {
    this.dialog.open<UploadImageDialogComponent, UploadImageDialogData,
      UploadImageDialogResult>(UploadImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        imageSubType: this.imageSubType
      }
    }).afterClosed().subscribe((result) => {
      if (result?.image) {
        if (this.selectionMode) {
          this.imageSelected.next(result.image);
        } else {
          if (this.isScada) {
            const type = imageResourceType(result.image);
            const key = encodeURIComponent(result.image.resourceKey);
            this.router.navigateByUrl(`resources/scada-symbols/${type}/${key}`);
          } else {
            this.updateData();
          }
        }
      }
    });
  }

  editImage($event: Event, image: ImageResourceInfo, itemIndex = -1) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isScada) {
      const type = imageResourceType(image);
      const key = encodeURIComponent(image.resourceKey);
      this.router.navigateByUrl(`resources/scada-symbols/${type}/${key}`);
    } else {
      this.dialog.open<ImageDialogComponent, ImageDialogData,
        ImageResourceInfo>(ImageDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          image,
          readonly: this.readonly(image)
        }
      }).afterClosed().subscribe((result) => {
        if (result) {
          this.imageUpdated(result, itemIndex);
        }
      });
    }
  }

  embedImage($event: Event, image: ImageResourceInfo, itemIndex = -1) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EmbedImageDialogComponent, EmbedImageDialogData,
      ImageResourceInfo>(EmbedImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        image,
        readonly: this.readonly(image)
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.imageUpdated(result, itemIndex);
      }
    });
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
