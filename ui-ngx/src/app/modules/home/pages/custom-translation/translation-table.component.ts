// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, debounceTime, distinctUntilChanged, map, share, takeUntil } from 'rxjs/operators';
import { TranslationInfo } from '@shared/models/custom-translation.model';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatSort, SortDirection } from '@angular/material/sort';
import { DialogService } from '@core/services/dialog.service';
import { isNotEmptyStr } from '@core/utils';
import { AddNewLanguageDialogComponent, AddNewLanguageDialogData } from './add-new-language-dialog.component';
import { ActivatedRoute, Router } from '@angular/router';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-translation-table',
    templateUrl: './translation-table.component.html',
    styleUrls: ['../../components/entity/entities-table.component.scss', './translation-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TranslationTableComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  textSearch = this.fb.control('', {nonNullable: true});
  textSearchMode = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  dataSource: TranslationInfoDatasource;
  pageLink: PageLink;

  displayedHeaderColumns: string[];
  displayedColumns: string[];

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private customTranslationService: CustomTranslationService,
              private translate: TranslateService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private router: Router,
              private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService) {
    super(store);
    const sortOrder: SortOrder = { property: 'localeCode', direction: Direction.ASC };
    this.pageLink = new PageLink(1024, 0, null, sortOrder);
    this.displayedHeaderColumns = ['localeCode', 'language', 'country', 'progress', 'actions'];
    this.displayedColumns = ['flag', 'localeCodeColumn', 'language', 'country', 'progress', 'actions'];
    this.dataSource = new TranslationInfoDatasource(this.customTranslationService);
  }

  ngOnInit() {
    this.sort.active = this.pageLink.sortOrder.property;
    this.sort.direction = (this.pageLink.sortOrder.direction).toLowerCase() as SortDirection;
    this.dataSource.loadTranslateInfo(this.pageLink);
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.pageLink.textSearch = isNotEmptyStr(value) ? value.trim() : null;
      this.updateData();
    });
    this.sort.sortChange.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageLink.sortOrder.property = this.sort.active;
      this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
      this.updateData();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByLocale(index: number, translate: TranslationInfo) {
    return translate.localeCode;
  }

  updateData(reload: boolean = false) {
    this.dataSource.loadTranslateInfo(this.pageLink, reload);
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

  flagIconClass(localeCode: string): string {
    return `fi fi-${localeCode.split('_')[1].toLowerCase()}`;
  }

  downloadLocale($event: Event, localeCode: string) {
    if ($event) {
      $event.stopPropagation();
    }
    this.customTranslationService.downloadFullTranslation(localeCode).subscribe(() => {});
  }

  deleteLocale($event: Event, translate: TranslationInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('custom-translation.delete-language-title', {language: translate.language}),
      this.translate.instant('custom-translation.delete-language-text', {language: translate.language}),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.customTranslationService.deleteCustomTranslation(translate.localeCode).subscribe(
          () => {
            this.updateData(true);
          }
        );
      }
    });
  }

  addNewLanguage($event: Event) {
    if($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddNewLanguageDialogComponent, AddNewLanguageDialogData>(AddNewLanguageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        langs: this.dataSource.currentTranslation().map(t => t.localeCode)
      }
    }).afterClosed().subscribe(result => {
      if (result) {
        this.updateData(true);
      }
    });
  }

  editLanguage($event: Event, translate: TranslationInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigate([translate.localeCode], {
      relativeTo: this.route,
      queryParams: {
        name: encodeURIComponent(translate.language),
        country: encodeURIComponent(translate.country)
      }
    }).then(() => {
    });
  }

  actionColumStyle() {
    const width = 48 * (this.readonly ? 2 : 3);
    return {width: `${width}px`, minWidth: `${width}px`, maxWidth: `${width}px`};
  }
}

export class TranslationInfoDatasource implements DataSource<TranslationInfo> {

  private translationInfo = new BehaviorSubject<TranslationInfo[]>([]);

  dataLoading = false;

  private allTranslation: Observable<Array<TranslationInfo>>;

  constructor(private customTranslationService: CustomTranslationService) {}

  connect(): Observable<TranslationInfo[] | ReadonlyArray<TranslationInfo>> {
    return this.translationInfo.asObservable();
  }

  disconnect() {
    this.translationInfo.complete();
  }

  loadTranslateInfo(pageLink: PageLink, reload: boolean = false): Observable<PageData<TranslationInfo>> {
    this.dataLoading = true;
    if (reload) {
      this.allTranslation = null;
    }
    const result = new ReplaySubject<PageData<TranslationInfo>>();
    this.fetchTranslation(pageLink).pipe(
      catchError(() => of(emptyPageData<TranslationInfo>())),
    ).subscribe((pageData) => {
        this.translationInfo.next(pageData.data);
        // this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoading = false;
      }
    );
    return result;
  }

  currentTranslation(): Array<TranslationInfo>{
    return this.translationInfo.value;
  }

  private fetchTranslation(pageLink: PageLink): Observable<PageData<TranslationInfo>> {
    return this.getAllTranslations().pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  private getAllTranslations(): Observable<Array<TranslationInfo>> {
    if (!this.allTranslation) {
      this.allTranslation = this.customTranslationService.getTranslationInfos().pipe(
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
    }
    return this.allTranslation;
  }

  isEmpty(): Observable<boolean> {
    return this.translationInfo.pipe(
      map((actions) => !actions.length)
    );
  }
}
