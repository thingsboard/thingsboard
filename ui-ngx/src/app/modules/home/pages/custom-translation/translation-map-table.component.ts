// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input, NgZone,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { MatSort, SortDirection } from '@angular/material/sort';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { BehaviorSubject, forkJoin, merge, Observable, of, Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { ActivatedRoute, QueryParamsHandling, Router } from '@angular/router';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, skip, takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull, isEqual, isNotEmptyStr, setByPath, unset } from '@core/utils';
import { DataSource } from '@angular/cdk/collections';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import {
  CustomTranslationEditData,
  CustomTranslationEditInfo,
  CustomTranslationState
} from '@shared/models/custom-translation.model';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { MatPaginator } from '@angular/material/paginator';
import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { environment as env } from '@env/environment';

interface CustomTranslationMap {
  key: string;
  original: string;
  translate: string;
  edit?: boolean;
  new?: boolean;
  untranslated?: boolean;
}

@Component({
    selector: 'tb-translation-map-table',
    templateUrl: './translation-map-table.component.html',
    styleUrls: ['../../components/entity/entities-table.component.scss', './translation-map-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TranslationMapTableComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort, {static: true}) sort: MatSort;
  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild('table', {read: ElementRef, static: true}) table: ElementRef;

  @Input()
  localeCode: string;

  @Input()
  localeName: string;

  @Input()
  @coerceBoolean()
  readonly: boolean;

  @Output()
  changeFullscreen = new EventEmitter<boolean>();

  textSearch = this.fb.control('', {nonNullable: true});
  textSearchMode = false;

  readonly defaultLang = env.defaultLang;

  private defaultPageSize = 50;
  pageSizeOptions = [this.defaultPageSize, this.defaultPageSize * 2, this.defaultPageSize * 3];

  dataSource: CustomTranslationMapDatasource;
  pageLink: PageLink;

  displayedColumns: string[];

  fullscreen = false;
  hidePageSize = false;

  CustomTranslationState = CustomTranslationState;

  newTranslated = false;

  newKey: FormGroup;

  filterParams = this.fb.control<Array<CustomTranslationState>>([]);

  private tableResize$: ResizeObserver;
  private destroy$ = new Subject<void>();
  private defaultSortOrder: SortOrder = { property: 'k', direction: Direction.ASC };

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private customTranslationService: CustomTranslationService,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private router: Router,
              private route: ActivatedRoute,
              private translate: TranslateService,
              private translateStore: TranslateStore,
              private zone: NgZone) {
    super(store);
    this.pageLink = new PageLink(this.defaultPageSize, 0, null, this.defaultSortOrder);
    this.displayedColumns = ['k', 'o', 't', 'action'];

    const { property, direction, page, pageSize, textSearch } = this.route.snapshot.queryParams;

    this.pageLink.sortOrder = {
      property: property || this.defaultSortOrder.property,
      direction: direction || this.defaultSortOrder.direction
    };

    this.pageLink.page = Number(page) || 0;
    this.pageLink.pageSize = Number(pageSize) || this.defaultPageSize;

    if (isNotEmptyStr(textSearch)) {
      const decodedTextSearch = decodeURI(textSearch).trim();
      this.textSearchMode = true;
      this.pageLink.textSearch = decodedTextSearch;
      this.textSearch.setValue(decodedTextSearch, {emitEvent: false});
    }
  }

  ngOnInit() {
    this.dataSource = new CustomTranslationMapDatasource(this.customTranslationService, this.localeCode, this.cd, this.translate, this.translateStore);
    this.sort.active = this.pageLink.sortOrder.property;
    this.sort.direction = (this.pageLink.sortOrder.direction).toLowerCase() as SortDirection;
    this.paginator.pageIndex = this.pageLink.page;
    this.paginator.pageSize = this.pageLink.pageSize;
    this.dataSource.loadTranslateInfo(this.pageLink, this.filterParams.value);
    this.initSubscriptions();

    this.newKey = this.fb.group({
      key: ['', Validators.required],
      original: ['', this.localeCode !== this.defaultLang ? [Validators.required] : []],
      translated: ['', Validators.required]
    });

    this.filterParams.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
        this.paginator.pageIndex = 0;
        this.updateData();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.tableResize$) {
      this.tableResize$.disconnect();
    }
  }

  trackByLocale(index: number, translate: CustomTranslationMap) {
    return translate.key;
  }

  private initSubscriptions() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      const queryParams: PageQueryParam = {
        textSearch: isNotEmptyStr(value) ? encodeURI(value) : null,
        page: null
      };
      this.paginator.pageIndex = 0;
      this.updatedRouterParams(queryParams);
    });

    const sortSubscription$: Observable<PageQueryParam> = this.sort.sortChange.asObservable().pipe(
      map((data) => {
        const direction: Direction = Direction[data.direction.toUpperCase()];
        const queryParams: PageQueryParam = {
          direction: this.defaultSortOrder.direction === direction ? null : direction,
          property: this.defaultSortOrder.property === data.active ? null : data.active,
          page: null
        };
        this.paginator.pageIndex = 0;
        return queryParams;
      })
    );

    const paginatorSubscription$: Observable<PageQueryParam> = this.paginator.page.asObservable().pipe(
      map((data) => ({
        page: data.pageIndex === 0 ? null : data.pageIndex,
        pageSize: data.pageSize === this.defaultPageSize ? null : data.pageSize
      }),
    ));

    merge(sortSubscription$, paginatorSubscription$).pipe(
      takeUntil(this.destroy$)
    ).subscribe(queryParams => {
      this.table.nativeElement.scrollIntoView(true);
      this.updatedRouterParams(queryParams);
    });

    this.route.queryParams.pipe(
      skip(1),
      takeUntil(this.destroy$)
    ).subscribe((params: PageQueryParam) => {
      const {page, pageSize, property, direction, textSearch} = params;
      Object.assign(this.pageLink, {
        page: Number(page) || 0,
        pageSize: Number(pageSize) || this.defaultPageSize,
        sortOrder: {
          property: property || this.defaultSortOrder.property,
          direction: (direction || this.defaultSortOrder.direction).toLowerCase()
        },
        textSearch: isNotEmptyStr(textSearch) ? decodeURI(textSearch).trim() : null
      });

      if (isNotEmptyStr(textSearch)) {
        this.textSearchMode = true;
        this.textSearch.setValue(decodeURI(textSearch), {emitEvent: false});
      } else {
        this.textSearch.reset('', {emitEvent: false});
      }

      this.updateData();
    });

    this.tableResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
    });
    this.tableResize$.observe(this.elementRef.nativeElement);
  }

  private updatedRouterParams(queryParams: PageQueryParam, queryParamsHandling: QueryParamsHandling = 'merge') {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling
    }).then(()=> {});
    if (queryParamsHandling === '' && isEqual(this.route.snapshot.queryParams, queryParams)) {
      this.updateData();
    }
  }

  private updateData(reload: boolean = false) {
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
    this.dataSource.loadTranslateInfo(this.pageLink, this.filterParams.value, reload);
  }

  enterSearchMode() {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitSearchMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
    this.changeFullscreen.emit(this.fullscreen);
  }

  toggleEditMode($event: Event, translation: CustomTranslationEditInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.readonly) {
      return;
    }
    this.toggleEditModeInElement(translation, $event.target as HTMLElement);
  }

  nextRowTab($event: Event, translate: CustomTranslationEditInfo) {
    const nextRow = ($event.target as HTMLElement).closest('mat-row').nextSibling;
    if (nextRow) {
      $event?.stopPropagation();
      const index = this.dataSource.currentTranslations().findIndex(t => t === translate);
      if (index !== -1 && index !== (this.pageLink.pageSize - 1)) {
        this.toggleEditModeInElement(this.dataSource.currentTranslations()[index + 1], nextRow as HTMLElement);
      }
    }
  }

  private toggleEditModeInElement(translation: CustomTranslationEditInfo, element: Element) {
    if (translation.s !== CustomTranslationState.Untranslated) {
      translation.edit = !translation.edit;
      if (translation.edit) {
        translation.value = translation.t;
        setTimeout(() => {
          element.closest('mat-row').querySelector('textarea').focus();
        });
      } else {
        delete translation.value;
      }
    }
  }

  toggleAddNewTranslated($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.newTranslated = !this.newTranslated;
  }

  deleteNewTranslated($event: Event, translate?: CustomTranslationEditInfo) {
    $event?.stopPropagation();
    if (translate) {
      this.customTranslationService.deleteCustomTranslationKey(this.localeCode, translate.k).subscribe(() => {
        if (isNotEmptyStr(translate.o) && this.localeCode !== this.defaultLang) {
          this.dataSource.updateTranslation(translate.k, '', CustomTranslationState.Added);
        } else {
          this.dataSource.deleteTranslation(translate.k);
        }
      });
    } else {
      this.resetNewKeyForm();
    }
  }

  private resetNewKeyForm() {
    this.newKey.reset();
    this.newKey.markAsPristine();
    this.newTranslated = false;
  }

  addNewTranslated() {
    if (this.newKey.valid && this.newKey.dirty) {
      const {key, translated} = this.newKey.value;
      let original = this.newKey.value.original;
      let observable: Observable<any>;
      if (this.localeCode === this.defaultLang) {
        observable = this.customTranslationService.patchCustomTranslation(this.localeCode, {[key]: translated});
        original = translated;
      } else {
        observable = forkJoin([
          this.customTranslationService.patchCustomTranslation(this.localeCode, {[key]: translated}),
          this.customTranslationService.patchCustomTranslation(this.defaultLang, {[key]: original})
        ]);
      }
      this.newKey.markAsPristine();
      observable.subscribe({
        next: () => {
          this.dataSource.addedTranslation(key, translated, original);
          this.resetNewKeyForm();
        },
        error: () => {
          this.newKey.markAsDirty();
        }
      });
    }
  }

  updatedTranslated(translate: CustomTranslationEditInfo) {
    if (!isNotEmptyStr(translate.value)) {
      this.resetTranslationEdit(translate);
      return;
    }
    const newValue = translate.value.trim();

    switch (translate.s) {
      case CustomTranslationState.Untranslated:
        this.customTranslationService.patchCustomTranslation(this.localeCode, {[translate.k]: newValue}).subscribe(() => {
          this.dataSource.updateTranslation(translate.k, newValue, CustomTranslationState.Customized);
          this.resetTranslationEdit(translate);
        });
        break;
      case CustomTranslationState.Added:
        this.customTranslationService.patchCustomTranslation(this.localeCode, {[translate.k]: newValue}).subscribe(() => {
          this.dataSource.updateTranslation(translate.k, newValue, CustomTranslationState.Added);
          this.resetTranslationEdit(translate);
        });
        break;
      case CustomTranslationState.Translated:
        if (translate.t.trim() !== newValue) {
          this.customTranslationService.patchCustomTranslation(this.localeCode, {[translate.k]: newValue}).subscribe(() => {
            this.dataSource.updateTranslation(translate.k, newValue, CustomTranslationState.Customized, true);
            this.resetTranslationEdit(translate);
          });
        } else {
          this.resetTranslationEdit(translate);
        }
        break;
      case CustomTranslationState.Customized:
        if (translate.p === newValue) {
          this.customTranslationService.deleteCustomTranslationKey(this.localeCode, translate.k).subscribe(() => {
            this.dataSource.updateTranslation(translate.k, newValue,
              isNotEmptyStr(newValue) ? CustomTranslationState.Translated : CustomTranslationState.Untranslated);
            this.resetTranslationEdit(translate);
          });
          break;
        } else if (translate.t.trim() !== newValue) {
          this.customTranslationService.patchCustomTranslation(this.localeCode, {[translate.k]: newValue}).subscribe(() => {
            this.dataSource.updateTranslation(translate.k, newValue, CustomTranslationState.Customized);
            this.resetTranslationEdit(translate);
          });
        } else {
          this.resetTranslationEdit(translate);
        }
        break;
    }
  }

  private resetTranslationEdit(translate: CustomTranslationEditInfo) {
    translate.edit = false;
    delete translate.value;
  }

  returnDefaultTranslation($event: Event, translation: CustomTranslationEditInfo) {
    $event?.stopPropagation();
    this.customTranslationService.deleteCustomTranslationKey(this.localeCode, translation.k).subscribe(() => {
      this.dataSource.updateTranslation(translation.k, translation.p,
        isNotEmptyStr(translation.p) ? CustomTranslationState.Translated : CustomTranslationState.Untranslated);
    });
  }
}

export class CustomTranslationMapDatasource implements DataSource<CustomTranslationEditInfo> {

  private translationInfo = new BehaviorSubject<CustomTranslationEditInfo[]>([]);
  private totalPageDataSubject = new BehaviorSubject<number>(0);

  dataLoading = false;

  private allTranslation: BehaviorSubject<Array<CustomTranslationEditInfo>>;

  private defaultLang = env.defaultLang;

  constructor(private customTranslationService: CustomTranslationService,
              private localeCode: string,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private translateStore: TranslateStore) {}

  connect(): Observable<CustomTranslationEditInfo[] | ReadonlyArray<CustomTranslationEditInfo>> {
    return this.translationInfo.asObservable();
  }

  disconnect() {
    this.translationInfo.complete();
    this.totalPageDataSubject.complete();
  }

  currentTranslations(): Array<CustomTranslationEditInfo> {
    return this.translationInfo.value;
  }

  loadTranslateInfo(pageLink: PageLink, filterParams: Array<CustomTranslationState>, reload: boolean = false) {
    this.dataLoading = true;
    if (reload) {
      this.allTranslation = null;
    }
    this.fetchTranslation(pageLink, filterParams).pipe(
      catchError(() => of(emptyPageData<CustomTranslationEditInfo>())),
    ).subscribe((pageData) => {
        this.translationInfo.next(pageData.data);
        this.totalPageDataSubject.next(pageData.totalElements);
        this.dataLoading = false;
        this.cd.markForCheck();
      }
    );
  }

  public updateTranslation(key: string, translate: string, state: CustomTranslationState, saveOrigin = false) {
    const translation = this.translationInfo.value.find(t => t.k === key);
    if (translation) {
      if (saveOrigin) {
        translation.p = translation.t;
      }
      if (this.localeCode === this.defaultLang) {
        translation.o = translate;
      }
      translation.t = translate;
      translation.s = state;
    }
    this.updateKeyInTranslationService(key, isNotEmptyStr(translate) ? translate : translation.o);
  }

  public addedTranslation(key: string, translate: string, original: string) {
    const translations = this.allTranslation.value;
    const translation = this.translationInfo.value.find(t => t.k === key);
    if (translation) {
      translation.t = translate;
      if (translation.s === CustomTranslationState.Added) {
        translation.o = original;
        translation.s = CustomTranslationState.Added;
      } else {
        translation.s = CustomTranslationState.Customized;
      }
    } else {
      const newTranslation: CustomTranslationEditInfo = {
        k: key,
        t: translate,
        o: original,
        s: CustomTranslationState.Added
      };
      translations.push(newTranslation);
      this.allTranslation.next(translations);
    }
    this.updateKeyInTranslationService(key, translate);
  }

  public deleteTranslation(key: string) {
    const translations = this.allTranslation.value.filter(t => t.k !== key);
    this.allTranslation.next(translations);
    this.updateKeyInTranslationService(key);
  }

  private fetchTranslation(pageLink: PageLink,
                           filterParams: Array<CustomTranslationState>): Observable<PageData<CustomTranslationEditInfo>> {
    return this.getAllTranslations().pipe(
      map((data) => {
        let filterData: CustomTranslationEditInfo[];
        if (filterParams.length && filterParams.length < 4) {
          const filterParamsSet = new Set(filterParams);
          filterData = data.filter(t => filterParamsSet.has(t.s));
        } else {
          filterData = data;
        }
        return pageLink.filterData(filterData);
      })
    );
  }

  private getAllTranslations(): Observable<Array<CustomTranslationEditInfo>> {
    if (!this.allTranslation) {
      this.allTranslation = new BehaviorSubject<Array<CustomTranslationEditInfo>>([]);
      this.customTranslationService.getTranslationForBasicEdit(this.localeCode).subscribe((value) => {
        this.allTranslation.next(this.flattenKeys(value));
      });
      return this.allTranslation.pipe(skip(1));
    }
    return this.allTranslation;
  }

  isEmpty(): Observable<boolean> {
    return this.translationInfo.pipe(
      map((actions) => !actions.length)
    );
  }

  total(): Observable<number> {
    return this.totalPageDataSubject;
  }

  private flattenKeys(obj: CustomTranslationEditData, path: string = ''): Array<CustomTranslationEditInfo> {
    return Object.keys(obj).reduce((acc: Array<CustomTranslationEditInfo>, key: string) => {
      const fullPath = path ? `${path}.${key}` : key;
      if (typeof obj[key] === 'object' && obj[key] !== null && !(obj[key].s)) {
        acc.push(...this.flattenKeys(obj[key] as any, fullPath));
      } else {
        acc.push(Object.assign(obj[key],  {k: fullPath}));
      }
      return acc;
    }, []);
  }

  private updateKeyInTranslationService(key: string, translate?: string) {
    if (!this.translateStore.hasTranslationFor(this.localeCode)) {
      return;
    }

    if (isDefinedAndNotNull(translate)) {
      setByPath(this.translateStore.getTranslations(this.localeCode), key, translate);
    } else {
      unset(this.translateStore.getTranslations(this.localeCode), key);
    }

    if (this.translate.getCurrentLang() === this.localeCode) {
      this.translateStore.setTranslations(this.localeCode, this.translateStore.getTranslations(this.localeCode), false);
      this.cd.detectChanges();
    }
  }
}
