// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  HostBinding,
  OnDestroy,
  OnInit,
  Renderer2,
  viewChild,
  viewChildren,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import {
  entityAliasesListToAliases,
  entityAliasesToList,
  filtersToReportDataFilterList,
  filterToReportDataFilter,
  HeaderFooter,
  isPdfReportTemplateConfig,
  PageOrientation,
  PageSize,
  paperSizeToPointsMap,
  PdfReportTemplateConfig,
  PdfReportTemplateSettings,
  reportDataFilterListToFilters,
  ReportRequest,
  ReportTemplate,
  ReportTemplateSettings,
  ReportTemplateType,
  TbReportFormat,
  toReportTemplateSettings,
  updateFromReportTemplateSettings,
  validateAndUpdateReportTemplate
} from '@shared/models/report.models';
import { ActivatedRoute } from '@angular/router';
import { ReportTemplateService } from '@core/http/report-template.service';
import { FiltersDialogComponent, FiltersDialogData } from '@home/components/filter/filters-dialog.component';
import { Filter, Filters } from '@shared/models/query/query.models';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import {
  ReportTemplateSettingsDialogComponent,
  ReportTemplateSettingsDialogData
} from '@home/pages/reporting/template/report-template-settings-dialog.component';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { FormBuilder, FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  assignReportComponent,
  editReportComponent,
  pointsToPixels,
  ReportComponentContext,
  reportComponentTypesData,
  ReportDragDropContext
} from '@home/pages/reporting/template/components/report-component.models';
import { EntityService } from '@core/http/entity.service';
import { IStateController, StateParams } from '@core/api/widget-api.models';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { AliasController } from '@core/api/alias-controller';
import { DialogService } from '@core/services/dialog.service';
import { ReportService } from '@core/http/report.service';
import { ReportComponentsComponent } from '@home/pages/reporting/template/components/report-components.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, skip, startWith, Subject } from 'rxjs';
import {
  EntityAliasDialogComponent,
  EntityAliasDialogData
} from '@home/components/alias/entity-alias-dialog.component';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { FilterDialogComponent, FilterDialogData } from '@home/components/filter/filter-dialog.component';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { DatePipe } from '@angular/common';
import { EntityId } from '@shared/models/id/entity-id';
import { CdkScrollable } from '@angular/cdk/overlay';
import {
  ReportTemplateHeaderFooterComponent
} from '@home/pages/reporting/template/report-template-header-footer.component';
import { MatButton, MatIconButton } from '@angular/material/button';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { BreadcrumbService } from '@core/services/breadcrumb.service';
import { HomeService } from '@core/services/home.service';

@Component({
    selector: 'tb-report-template-page',
    templateUrl: './report-template-page.component.html',
    styleUrls: ['./report-template-page.component.scss', './report-components-container.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportTemplatePageComponent extends PageComponent
  implements OnInit, AfterViewInit, OnDestroy, HasDirtyFlag {

  TbReportFormat = TbReportFormat;

  reportComponentTypesData = reportComponentTypesData;

  get isDirty(): boolean {
    return this.isDirtyValue;
  }

  set isDirty(value: boolean) {
    this.isDirtyValue = value;
  }

  get showHeaderFooter(): boolean {
    return !this.subReport && this.format === TbReportFormat.PDF;
  }

  get pdfConfiguration(): PdfReportTemplateConfig {
    return isPdfReportTemplateConfig(this.reportTemplate?.configuration) ? this.reportTemplate.configuration : null;
  }

  get currentHeader(): HeaderFooter {
    return this.headerToggleValue === 'header' ? this.pdfConfiguration?.header : this.pdfConfiguration?.header?.firstPage;
  }

  get currentFooter(): HeaderFooter {
    return this.footerToggleValue === 'header' ? this.pdfConfiguration?.footer : this.pdfConfiguration?.footer?.firstPage;
  }

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  reportComponentsComponents = viewChildren(ReportComponentsComponent);

  headerFooterComponents = viewChildren(ReportTemplateHeaderFooterComponent);

  reportTemplateContainerEl = viewChild('reportTemplateContainer', {
    read: ElementRef<HTMLElement>,
  });

  reportTemplateContentEl = viewChild('reportTemplateContent', {
    read: CdkScrollable,
  });

  reportTemplateLayoutEl = viewChild('reportTemplateLayout', {
    read: ElementRef<HTMLElement>,
  });

  readonly = false;

  format: TbReportFormat;

  subReport = false;

  isDirtyValue: boolean;

  isFullscreen = false;

  reportTemplate: ReportTemplate;

  timeDataPattern: string;

  updateBreadcrumbs = new EventEmitter();

  prevReportComponent: ReportComponentConfig;
  editingReportComponent: ReportComponentConfig;

  reportComponentSearchFormControl: FormControl;
  reportComponentsFilter = '';
  reportTemplateSettingsFormControl: FormControl;

  headerToggleValue: 'header' | 'firstPageHeader' = 'header';
  footerToggleValue: 'header' | 'firstPageHeader' = 'header';

  reportComponentContext: ReportComponentContext;

  pageWidth: number;

  marginLeft: number;
  marginRight: number;

  contentMarginTop: number;
  contentMarginBottom: number;

  headerMarginTop: number;
  footerMarginBottom: number;

  background: string;

  scale = 1;

  layoutWidth: number;

  viewInited = false;
  hasScroll = false;
  scrollTop = false;

  private scrolling = false;

  private layoutResize$: ResizeObserver;

  private timeDataPatternSubject = new Subject<string>();

  timeDataPattern$ = this.timeDataPatternSubject.asObservable();

  breadcrumbs$ = this.breadcrumbService.breadcrumbs$;

  // @ts-ignore
  private stateController: IStateController = {
    getStateParams: (): StateParams => ({}),
    getEntityId: (): EntityId => null
  };

  constructor(private route: ActivatedRoute,
              private reportTemplateService: ReportTemplateService,
              private reportService: ReportService,
              private entityService: EntityService,
              private utils: UtilsService,
              private translate: TranslateService,
              private destroyRef: DestroyRef,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private fb: FormBuilder,
              private date: DatePipe,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private popoverService: TbPopoverService,
              private viewContainerRef: ViewContainerRef,
              private breadcrumbService: BreadcrumbService,
              public homeService: HomeService) {
    super();
  }

  ngOnInit() {
    this.homeService.setHideMainToolbar(true);
    this.reportComponentContext = {
      translate: this.translate,
      utils: this.utils,
      entityService: this.entityService,
      aliasController: null,
      aliasAndFilterCallbacks: {
        createEntityAlias: this.createEntityAlias.bind(this),
        editEntityAlias: this.editEntityAlias.bind(this),
        createFilter: this.createFilter.bind(this)
      },
      format: null,
      dragDropCtx: new ReportDragDropContext()
    };
    this.reportComponentSearchFormControl = this.fb.control('', {nonNullable: true});
    this.reportComponentSearchFormControl.valueChanges.pipe(
      debounceTime(150),
      startWith(''),
      distinctUntilChanged((a: string, b: string) => a.trim() === b.trim()),
      skip(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((search) => {
      this.reportComponentsFilter = search;
    });
    this.reportTemplateSettingsFormControl = this.fb.control(null);
    this.reportTemplateSettingsFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((settings: PdfReportTemplateSettings) => {
      this.updateReportTemplateSettings(settings);
    });
    this.route.data.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        const {reportTemplate, readonly} = this.route.snapshot.data.reportTemplate;
        this.readonly = readonly ?? false;
        this.init(reportTemplate);
      }
    );
  }

  ngAfterViewInit() {
    this.layoutResize$ = new ResizeObserver(() => {
      this.layoutResize();
    });
    setTimeout(() => {
      this.layoutResize$.observe(this.reportTemplateLayoutEl().nativeElement);
      this.viewInited = true;
    });
    const reportTemplateContent = this.reportTemplateContentEl();
    if (reportTemplateContent) {
      reportTemplateContent.elementScrolled().pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        if (this.hasScroll) {
          const bottomOffset = reportTemplateContent.measureScrollOffset('bottom');
          const topOffset = reportTemplateContent.measureScrollOffset('top');
          if (this.scrolling && (bottomOffset === 0 || topOffset === 0)) {
            this.scrolling = false;
          }
          const scrollTop = bottomOffset === 0;
          if (this.scrollTop !== scrollTop && !this.scrolling) {
            this.scrollTop = scrollTop;
            this.cd.markForCheck();
          }
        }
      })
    }
  }

  ngOnDestroy() {
    if (this.layoutResize$) {
      this.layoutResize$.disconnect();
    }
  }

  saveReportTemplate() {
    this.reportTemplateService.saveReportTemplate(this.reportTemplate).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  declineReportTemplate() {
    this.reportTemplateService.getReportTemplate<PdfReportTemplateConfig>(this.reportTemplate.id.id).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  public currentHeaderChanged(value: 'header' | 'firstPageHeader') {
    this.headerToggleValue = value;
    this.updatePageLayout();
  }

  public currentFooterChanged(value: 'header' | 'firstPageHeader') {
    this.footerToggleValue = value;
    this.updatePageLayout();
  }

  public enabledHeaderFooterChanged() {
    this.updatePageLayout();
    this.isDirty = true;
  }

  public headerFooterExpandAnimationStart() {
    this.layoutResize$.unobserve(this.reportTemplateLayoutEl().nativeElement);
  }

  public headerFooterExpandAnimationFinish() {
    this.layoutResize$.observe(this.reportTemplateLayoutEl().nativeElement);
  }

  public reportComponentsChanged(): void {
    this.updatePageLayout();
    this.isDirty = true;
    setTimeout(() => {
      this.selectEditingComponent();
    });
  }

  public reportComponentRemoved(reportComponent: ReportComponentConfig) {
    if (this.editingReportComponent === reportComponent) {
      this.cancelReportComponentEdit();
    }
  }

  public editReportComponent(reportComponent: ReportComponentConfig): void {
    if (this.editingReportComponent !== reportComponent) {
      this.editingReportComponent = reportComponent;
      this.prevReportComponent = editReportComponent(reportComponent);
      this.selectEditingComponent();
      this.renderer.addClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
    }
  }

  public reportComponentUpdated(setDirty = true) {
    if (this.editingReportComponent) {
      const reportComponentsComponents = this.allReportComponentsComponents();
      for (const component of reportComponentsComponents) {
        if (component.componentUpdated(this.editingReportComponent)) {
          break;
        }
      }
    }
    if (setDirty) {
      this.isDirty = true;
    }
  }

  public saveReportComponent(): void {
    this.prevReportComponent = null;
    this.editingReportComponent = null;
    const reportComponentsComponents = this.allReportComponentsComponents();
    for (const component of reportComponentsComponents) {
      component.componentSelected(null);
    }
    this.renderer.removeClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
  }

  public cancelReportComponentEdit(): void {
    if (this.editingReportComponent) {
      assignReportComponent(this.editingReportComponent, this.prevReportComponent);
      this.reportComponentUpdated(false);
      this.prevReportComponent = null;
      this.editingReportComponent = null;
      const reportComponentsComponents = this.allReportComponentsComponents();
      for (const component of reportComponentsComponents) {
        component.componentSelected(null);
      }
      this.renderer.removeClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
    }
  }

  public openFilters($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const reportDataFilters = deepClone(this.reportTemplate.configuration.filters);
    const filters = reportDataFilterListToFilters(reportDataFilters);
    const headerComponents = this.pdfConfiguration?.header?.components ?? [];
    const footerComponents = this.pdfConfiguration?.footer?.components ?? [];
    this.dialog.open<FiltersDialogComponent, FiltersDialogData,
      Filters>(FiltersDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filters,
        disableUserEdit: true,
        widgets: [],
        isSingleFilter: false,
        reportMode: true,
        reportComponents: [
          ...headerComponents,
          ...this.reportTemplate.configuration.components,
          ...footerComponents
        ]
      }
    }).afterClosed().subscribe((filters) => {
      if (filters) {
        this.reportTemplate.configuration.filters = filtersToReportDataFilterList(filters);
        this.reportComponentContext.aliasController.updateFilters(filters);
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  public toggleVersionControl($event: Event, versionControlButton: MatButton | MatIconButton) {
    $event?.stopPropagation();
    const trigger = versionControlButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const versionControlPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: VersionControlComponent,
        preferredPlacement: 'leftTop',
        context: {
          detailsMode: true,
          active: true,
          singleEntityMode: true,
          externalEntityId: this.reportTemplate.externalId || this.reportTemplate.id,
          entityId: this.reportTemplate.id,
          entityName: this.reportTemplate.name,
          onBeforeCreateVersion: () => this.reportTemplateService.saveReportTemplate(this.reportTemplate).pipe(
            tap((reportTemplate) => {
              this.init(reportTemplate);
            })
          )
        }
      });
      versionControlPopover.tbComponentRef.instance.popoverComponent = versionControlPopover;
      versionControlPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
        this.reportTemplateService.getReportTemplate(this.reportTemplate.id.id).subscribe(reportTemplate => {
          versionControlPopover.hide();
          this.init(reportTemplate);
        });
      });
    }
  }

  private createFilter(filter: string): Observable<Filter> {
    const singleFilter: Filter = {id: null, filter, keyFilters: [], editable: true};
    const reportDataFilters = deepClone(this.reportTemplate.configuration.filters);
    const filters = reportDataFilterListToFilters(reportDataFilters);
    return this.dialog.open<FilterDialogComponent, FilterDialogData,
      Filter>(FilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true,
        filters,
        filter: singleFilter
      }
    }).afterClosed().pipe(
      tap((result) => {
        if (result) {
          const reportDataFilter = filterToReportDataFilter(result);
          this.reportTemplate.configuration.filters.push(reportDataFilter);
          const updatedFilters = reportDataFilterListToFilters(this.reportTemplate.configuration.filters);
          this.reportComponentContext.aliasController.updateFilters(updatedFilters);
        }
      })
    );
  }

  public openEntityAliases($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const entityAliasesList = deepClone(this.reportTemplate.configuration.entityAliases);
    const entityAliases = entityAliasesListToAliases(entityAliasesList);
    const headerComponents = this.pdfConfiguration?.header?.components ?? [];
    const footerComponents = this.pdfConfiguration?.footer?.components ?? [];
    this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases,
        widgets: [],
        disableResolveMultiple: true,
        isSingleEntityAlias: false,
        reportMode: true,
        subReport: this.subReport,
        reportComponents: [
          ...headerComponents,
          ...this.reportTemplate.configuration.components,
          ...footerComponents
        ]
      }
    }).afterClosed().subscribe((entityAliases) => {
      if (entityAliases) {
        this.reportTemplate.configuration.entityAliases = entityAliasesToList(entityAliases);
        this.reportComponentContext.aliasController.updateEntityAliases(entityAliases);
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  private createEntityAlias(alias: string, allowedEntityTypes: Array<EntityType>): Observable<EntityAlias> {
    const singleEntityAlias: EntityAlias = {id: null, alias, filter: {resolveMultiple: false}};
    const entityAliasesList = deepClone(this.reportTemplate.configuration.entityAliases);
    const entityAliases = entityAliasesListToAliases(entityAliasesList);
    return this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true,
        allowedEntityTypes,
        entityAliases,
        alias: singleEntityAlias,
        reportMode: true,
        disableResolveMultiple: true,
        subReport: this.subReport
      }
    }).afterClosed().pipe(
      tap((entityAlias) => {
        if (entityAlias) {
          this.reportTemplate.configuration.entityAliases.push(entityAlias);
          const updatedEntityAliases = entityAliasesListToAliases(this.reportTemplate.configuration.entityAliases);
          this.reportComponentContext.aliasController.updateEntityAliases(updatedEntityAliases);
          this.isDirty = true;
          this.cd.markForCheck();
        }
      })
    );
  }

  private editEntityAlias(alias: EntityAlias, allowedEntityTypes: Array<EntityType>): Observable<EntityAlias> {
    const entityAliasesList = deepClone(this.reportTemplate.configuration.entityAliases);
    const entityAliases = entityAliasesListToAliases(entityAliasesList);
    return this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: false,
        allowedEntityTypes,
        entityAliases,
        alias: deepClone(alias),
        reportMode: true,
        disableResolveMultiple: true,
        subReport: this.subReport
      }
    }).afterClosed().pipe(
      tap((entityAlias) => {
        if (entityAlias) {
          const index = this.reportTemplate.configuration.entityAliases.findIndex(alias => alias.id === entityAlias.id);
          if (index > -1) {
            this.reportTemplate.configuration.entityAliases[index] = entityAlias;
            const updatedEntityAliases = entityAliasesListToAliases(this.reportTemplate.configuration.entityAliases);
            this.reportComponentContext.aliasController.updateEntityAliases(updatedEntityAliases);
            this.isDirty = true;
            this.cd.markForCheck();
          }
        }
      })
    );
  }

  public openReportTemplateSettings($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const settings = toReportTemplateSettings(this.reportTemplate);
    this.dialog.open<ReportTemplateSettingsDialogComponent, ReportTemplateSettingsDialogData,
      ReportTemplateSettings>(ReportTemplateSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        subReport: this.subReport,
        format: this.format,
        settings
      }
    }).afterClosed().subscribe((settings) => {
      if (settings) {
        this.updateReportTemplateSettings(settings);
        this.reportTemplateSettingsFormControl.patchValue(settings, {emitEvent: false});
      }
    });
  }

  generateTestReport() {
    const reportRequest: ReportRequest = {
      reportTemplateConfig: this.reportTemplate.configuration,
      timezone: getDefaultTimezone()
    };
    this.dialogService.progress(
      this.reportService.downloadTestReport(reportRequest, this.format !== TbReportFormat.PDF), this.translate.instant('report.generating-report')).subscribe();
  }

  scrollBottomTop() {
    const reportTemplateContent = this.reportTemplateContentEl();
    if (reportTemplateContent) {
      this.scrolling = true;
      if (this.scrollTop) {
        reportTemplateContent.scrollTo({top: 0, behavior: 'smooth'});
        this.scrollTop = false;
      } else {
        reportTemplateContent.scrollTo({bottom: 0, behavior: 'smooth'});
        this.scrollTop = true;
      }
    }
  }

  toggleSidenav() {
    this.homeService.toggleSideBar.emit();
  }

  private updateReportTemplateSettings(settings: ReportTemplateSettings): void {
    this.timeDataPattern = settings.timeDataPattern;
    this.timeDataPatternSubject.next(this.timeDataPattern);
    updateFromReportTemplateSettings(this.reportTemplate, settings);
    this.updatePageLayout();
    this.isDirty = true;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }

  private updatePageLayout() {
    let pageSize: PageSize;
    let orientation: PageOrientation;
    if (this.subReport || !isPdfReportTemplateConfig(this.reportTemplate.configuration)) {
      pageSize = PageSize.A4;
      orientation = PageOrientation.PORTRAIT;
      this.background = '#fff';
      this.marginLeft = 20;
      this.marginRight = 20;
      this.contentMarginTop = 20;
      this.contentMarginBottom = 20;
    } else {
      pageSize = this.reportTemplate.configuration.pageSize;
      orientation = this.reportTemplate.configuration.pageOrientation;
      this.background = this.reportTemplate.configuration.pageBackground;
      this.marginLeft = this.reportTemplate.configuration.pageMargins.left;
      this.marginRight = this.reportTemplate.configuration.pageMargins.right;
      if (this.currentHeader?.enabled && this.currentHeader.components?.length) {
        this.headerMarginTop = this.reportTemplate.configuration.pageMargins.top;
        this.contentMarginTop = 0;
      } else {
        this.headerMarginTop = 0;
        this.contentMarginTop = this.reportTemplate.configuration.pageMargins.top;
      }

      if (this.currentFooter?.enabled && this.currentFooter.components?.length) {
        this.footerMarginBottom = this.reportTemplate.configuration.pageMargins.bottom;
        this.contentMarginBottom = 0;
      } else {
        this.footerMarginBottom = 0;
        this.contentMarginBottom = this.reportTemplate.configuration.pageMargins.bottom;
      }
    }

    const pageSizePoints = paperSizeToPointsMap.get(pageSize);
    this.pageWidth = orientation === PageOrientation.PORTRAIT ? pageSizePoints[0] : pageSizePoints[1];
    this.updateScale();
  }

  private layoutResize() {
    const reportTemplateContent = this.reportTemplateContentEl();
    if (!reportTemplateContent) { return; }
    const bottomOffset = reportTemplateContent.measureScrollOffset('bottom');
    const topOffset = reportTemplateContent.measureScrollOffset('top');
    const hasScroll = bottomOffset > 0 || topOffset > 0;
    if (this.hasScroll !== hasScroll) {
      this.hasScroll = hasScroll;
      this.scrollTop = false;
      this.cd.markForCheck();
    }
    this.layoutWidth = this.reportTemplateLayoutEl().nativeElement.getBoundingClientRect().width;
    const reportComponentsComponents = this.allReportComponentsComponents();
    reportComponentsComponents.forEach(component => {
      this.renderer.setStyle(component.element.nativeElement, 'maxWidth', this.layoutWidth + 'px');
    });
    this.updateScale();
  }

  private updateScale() {
    if (this.pageWidth && this.layoutWidth) {
      const pageWidthPx = pointsToPixels(this.pageWidth);
      if (pageWidthPx > this.layoutWidth) {
        this.scale = this.layoutWidth / pageWidthPx;
      } else {
        this.scale = 1;
      }
    } else {
      this.scale = 1;
    }
    this.cd.markForCheck();
  }

  private init(reportTemplate: ReportTemplate) {
    this.cancelReportComponentEdit();
    this.headerToggleValue = 'header';
    this.footerToggleValue = 'header';
    this.reportTemplate = validateAndUpdateReportTemplate(reportTemplate);
    this.format = this.reportTemplate.format;
    this.subReport = this.reportTemplate.type === ReportTemplateType.SUB_REPORT;

    this.updatePageLayout();

    const entityAliases = entityAliasesListToAliases(this.reportTemplate.configuration.entityAliases);
    const filters = reportDataFilterListToFilters(this.reportTemplate.configuration.filters);

    this.reportComponentContext.aliasController = new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => this.stateController,
      entityAliases,
      filters
    );
    this.reportComponentContext.format = this.format;

    const settings = toReportTemplateSettings(this.reportTemplate);

    this.timeDataPattern = settings.timeDataPattern;

    this.reportComponentSearchFormControl.reset();
    this.reportTemplateSettingsFormControl.patchValue(settings, {emitEvent: false});
    if (this.readonly) {
      this.reportTemplateSettingsFormControl.disable({emitEvent: false});
    }
    this.isDirty = false;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }

  private allReportComponentsComponents(): ReportComponentsComponent[] {
    const result = [...this.reportComponentsComponents()];
    const headerFooterComponents = this.headerFooterComponents();
    headerFooterComponents.forEach(headerFooter => {
      const components = headerFooter.reportComponentsComponents();
      result.push(...components);
    });
    return result;
  }

  private selectEditingComponent() {
    if (this.editingReportComponent) {
      const reportComponentsComponents = this.allReportComponentsComponents();
      for (const component of reportComponentsComponents) {
        component.componentSelected(this.editingReportComponent);
      }
    }
  }
}
